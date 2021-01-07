

import React, { Component } from 'react';
import { SafeAreaView, Modal, Platform } from 'react-native'; 
import WebView  from './WebView'; 
import UserInfo from './UserInfo'

export class BootpayWebView extends Component { 
    state = {
        visibility: false,
        script: '',
        firstLoad: false
    }

    async componentWillUnmount() { 
        this.setState(
            {
                visibility: false,
                script: '',
                firstLoad: false
            }
        )
        UserInfo.setBootpayLastTime(Date.now());
    }

    render() { 

        const injectedJavascript = `(function() {
                window.postMessage = function(data) {
            window.ReactNativeWebView.postMessage(data);
            };
        })()`

        return <Modal
            animationType={'slide'}
            transparent={false}
            visible={this.state.visibility}>
            <SafeAreaView style={{ flex: 1 }}>
                <WebView
                    ref={(wv) => this.wv = wv}
                    useWebKit={true}
                    originWhitelist={['*']}
                    source={{
                        uri: 'https://inapp.bootpay.co.kr/3.3.1/production.html'
                    }}
                    javaScriptEnabled={true}
                    javaScriptCanOpenWindowsAutomatically={true}
                    scalesPageToFit={true}
                    onLoadEnd={this.onLoadEnd}
                    onMessage={this.onMessage}
                    onShouldStartLoadWithRequest={this.onShouldStartLoadWithRequest}
                />
            </SafeAreaView>

        </Modal>
    }

    request = (payload, items, user, extra) => {
        payload.application_id =  Platform.OS == 'ios' ? this.props.ios_application_id : this.props.android_application_id;
        payload.items = items;
        payload.user = user;
        payload.extra = extra;

        //visibility가 true가 되면 webview onLoaded가 실행됨        
        this.setState(
            {
                visibility: true,
                script: 'if(BootPay == undefined || BootPay.request == undefined) {return;} BootPay.request(' + JSON.stringify(payload) + ')',
                firstLoad: false 
            }
        ) 
        UserInfo.updateInfo();
    }

    dismiss = () => {
        this.setState(
            ({ visibility }) => ({
                visibility: false
            })
        )
        this.removePaymentWindow();
    }

    // uri: 'https://inapp.bootpay.co.kr/3.3.1/production.html'

    onLoadEnd = async (e) => { 

        if(this.state.firstLoad == true) return;
        this.setBootpayPlatform();
        await this.setAnalyticsData();    
        this.goBootpayRequest();

        this.setState({
            ...this,
            firstLoad: true 
        })
    }


    generateScript= (script) => {
        const onError = '.error(function(data){ window.ReactNativeWebView.postMessage( JSON.stringify(data) ); })';
        const onCancel = '.cancel(function(data){ window.ReactNativeWebView.postMessage( JSON.stringify(data) ); })';
        const onReady = '.ready(function(data){ window.ReactNativeWebView.postMessage( JSON.stringify(data) ); })';
        const onConfirm = '.confirm(function(data){ window.ReactNativeWebView.postMessage( JSON.stringify(data) ); })';
        const onClose = '.close(function(data){ window.ReactNativeWebView.postMessage("close"); })';
        

        return script + onError + onCancel + onReady + onConfirm + onClose + '; void(0);';
    }

    onMessage = ({ nativeEvent }) => {
        if (nativeEvent == undefined || nativeEvent.data == undefined) return;

        if(nativeEvent.data == 'close') {
            if(this.props.onClose == undefined) return;
            json = {
                action: 'BootpayClose',
                message: '결제창이 닫혔습니다'
            }
            this.props.onClose(json);
            this.dismiss();
            return;
        }

        const data = JSON.parse(nativeEvent.data);  
        switch (data.action) {
            case 'BootpayCancel':
                if(this.props.onCancel != undefined) this.props.onCancel(data);
                this.setState(
                    {
                        visibility: false 
                    }
                ) 
                break;
            case 'BootpayError':
                if(this.props.onError != undefined) this.props.onError(data);
                this.setState(
                    {
                        visibility: false 
                    }
                ) 
                break;
            case 'BootpayBankReady':
                if(this.props.onReady != undefined) this.props.onReady(data);
                break;
            case 'BootpayConfirm':
                if(this.props.onConfirm != undefined) this.props.onConfirm(data);
                break;
            case 'BootpayDone':
                if(this.props.onDone != undefined) this.props.onDone(data);
                this.setState(
                    {
                        visibility: false 
                    }
                ) 
                break; 
        } 
    }

    onShouldStartLoadWithRequest = (url) => { 
        return true; 
    }

    setBootpayPlatform = () => {
        if(Platform.OS == 'ios') {
            this.injectJavaScript(`
  BootPay.setDevice('IOS');
          `);
        } else if(Platform.OS == 'android'){
            this.injectJavaScript(`
  BootPay.setDevice('ANDROID');
          `);
        } 

    }

    goBootpayRequest = () => {
        const fullScript = this.generateScript(this.state.script); 
        this.injectJavaScript(fullScript);
    }

    transactionConfirm = (data) => { 
        var json = JSON.stringify(data)  
        this.injectJavaScript(`
        BootPay.transactionConfirm(${json});
          `);
    }

    removePaymentWindow = () => {
        this.injectJavaScript(`
        BootPay.removePaymentWindow();
          `);
    }

    injectJavaScript = (script) => {
        if(this.wv == null || this.wv == undefined) return;
        this.wv.injectJavaScript(`
        javascript:(function(){${script} })()
          `); 
    }

    setAnalyticsData = async () => {
        const uuid = await UserInfo.getBootpayUUID();
        const bootpaySK = await UserInfo.getBootpaySK();
        const bootLastTime = await UserInfo.getBootpayLastTime();
 

        const elaspedTime = Date.now() - bootLastTime;
        this.injectJavaScript(` 
        window.BootPay.setAnalyticsData({uuid:'${uuid}',sk:'${bootpaySK}',sk_time:${bootLastTime},time:${elaspedTime}});
        `); 
    }
}

// BootpayWebView.prototype = 