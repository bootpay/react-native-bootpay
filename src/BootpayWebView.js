

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
                firstLoad: false,
                showCloseButton: false
            }
        )
        UserInfo.setBootpayLastTime(Date.now());
    }

    render() {

        const injectedJavascript = `(function() {
                window.postMessage = function(data) {
            window.BootpayRNWebView.postMessage(data);
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
                        uri: 'https://inapp.bootpay.co.kr/3.3.2/production.html'
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

        var showCloseBtn = false;

        if(extra != undefined) {
            if(extra.quickPopup == 1) {
                this.appendJavaScriptBeforeContentLoaded('BootPay.startQuickPopup();');
            }
            if(Platform.OS == 'ios' && extra.ios_close_button == true) {
                showCloseBtn = true;
                // if(this.wv == null || this.wv == undefined) return;
                // this.wv.showCloseButton();
            }
        }

        //visibility가 true가 되면 webview onLoaded가 실행됨
        this.setState(
            {
                visibility: true,
                script: 'if(BootPay == undefined || BootPay.request == undefined) {return;} BootPay.request(' + JSON.stringify(payload) + ')',
                firstLoad: false,
                showCloseButton: showCloseBtn
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
        this.setPayScript();
        this.startBootpay();

        this.setState({
            ...this,
            firstLoad: true
        }) 
    }


    generateScript= (script) => {
        const onError = '.error(function(data){ window.BootpayRNWebView.postMessage( JSON.stringify(data) ); })';
        const onCancel = '.cancel(function(data){ window.BootpayRNWebView.postMessage( JSON.stringify(data) ); })';
        const onReady = '.ready(function(data){ window.BootpayRNWebView.postMessage( JSON.stringify(data) ); })';
        const onConfirm = '.confirm(function(data){ window.BootpayRNWebView.postMessage( JSON.stringify(data) ); })';
        const onClose = '.close(function(data){ window.BootpayRNWebView.postMessage("close"); })';
        const onDone = '.done(function(data){ window.BootpayRNWebView.postMessage( JSON.stringify(data) ); })';


        console.log(script);

        return script + onError + onCancel + onReady + onConfirm + onClose + onDone + '; void(0);';
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

    setBootpayPlatform = async () => { 
        if(Platform.OS == 'ios') {
            this.appendJavaScriptBeforeContentLoaded(`BootPay.setDevice('IOS');`); 
        } else if(Platform.OS == 'android'){
            this.appendJavaScriptBeforeContentLoaded(`BootPay.setDevice('ANDROID');`); 
        }

    }

    setPayScript = () => {
        const fullScript = this.generateScript(this.state.script);
        this.injectJavaScript(fullScript);
        if(this.state.showCloseButton == true) {
            if(this.wv == null || this.wv == undefined) return; 
            this.wv.showCloseButton();
        }

    }

    startBootpay = () => {
        this.wv.startBootpay(); 
    } 

    transactionConfirm = (data) => {
        var json = JSON.stringify(data)
        this.callJavaScript(`
        BootPay.transactionConfirm(${json});
          `);
    }

    removePaymentWindow = () => {
        this.callJavaScript(`
        BootPay.removePaymentWindow();
          `);
    }

    injectJavaScript = (script) => {
        if(this.wv == null || this.wv == undefined) return;
        this.wv.injectJavaScript(`
        javascript:(function(){${script} })()
          `);
    }

    callJavaScript = (script) => {
        if(this.wv == null || this.wv == undefined) return;
        this.wv.callJavaScript(`
        javascript:(function(){${script} })()
          `);
    }

    appendJavaScriptBeforeContentLoaded = (script) => {
        if(this.wv == null || this.wv == undefined) return;
        this.wv.appendJavaScriptBeforeContentLoaded(`${script}`);
    }
 

    setAnalyticsData = async () => { 
        const uuid = await UserInfo.getBootpayUUID(); 
        const bootpaySK = await UserInfo.getBootpaySK();
        const bootLastTime = await UserInfo.getBootpayLastTime();      


        const elaspedTime = Date.now() - bootLastTime;
        this.appendJavaScriptBeforeContentLoaded(`window.BootPay.setAnalyticsData({uuid:'${uuid}',sk:'${bootpaySK}',sk_time:${bootLastTime},time:${elaspedTime}});`); 
    }
}

// BootpayWebView.prototype =
