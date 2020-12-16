import React, { Component } from 'react';
import { SafeAreaView, Modal, Platform } from 'react-native'; 
import WebView  from './WebView'; 
import UserInfo from './UserInfo'

export default class BootpayWebView extends Component { 
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
                break;
            case 'BootpayError':
                if(this.props.onError != undefined) this.props.onError(data);
                break;
            case 'BootpayBankReady':
                if(this.props.onReady != undefined) this.props.onReady(data);
                break;
            case 'BootpayConfirm':
                if(this.props.onConfirm != undefined) this.props.onConfirm(data);
                break;
            case 'BootpayDone':
                if(this.props.onDone != undefined) this.props.onDone(data);
                break; 
        } 
    }

    onShouldStartLoadWithRequest = (url) => { 
        return true;
        // goBootpayRequest();
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
        // const script = 'BootPay.request({price: "1000.0",tax_free: "0.0",application_id: "5b8f6a4d396fa665fdc2b5e9",name: "테스트 마스카라",pg:"kcp",phone:"010-1234-4567",show_agree_window: 0,items: [{item_name: "미키 마우스",qty: 1,unique: "ITEM_CODE_MOUSE",price: 1000,cat1: "",cat2: "",cat3: ""},{item_name: "키보드",qty: 1,unique: "ITEM_CODE_KEYBOARD",price: 10000,cat1: "패션",cat2: "여성상의",cat3: "블라우스"}],params: {"callbackParam2":"value34","callbackParam1":"value12","callbackParam3":"value56","callbackParam4":"value78"},order_id: "1234_1234_124",use_order_id: "false",account_expire_at: "2020-12-07",method: "card",user_info: {id: "",username: "",user_id: "",email: "user1234@gmail.com",gender: 0,birth: "",phone: "010-1234-4567",area: "서울",addr: "서울시 동작구 상도로"},extra: {app_scheme:"bootpaysample",expire_month:"0",vbank_result:true,start_at: "",end_at: "",quota:"0,1,2,3",offer_period: "",popup: 0,quick_popup: 0,locale:"ko",disp_cash_result:"Y",escrow:"0",theme:"purple",custom_background:"",custom_font_color:"",iosCloseButton: false}})'
        const fullScript = this.generateScript(this.state.script);
        // console.log(fullScript);
        this.injectJavaScript(fullScript);
    }

    transactionConfirm = (data) => {
        this.injectJavaScript(`
        var data = JSON.parse(${data}); 
        BootPay.transactionConfirm(data);
          `);
    }

    removePaymentWindow = () => {
        this.injectJavaScript(`
        BootPay.removePaymentWindow();
          `);
    }

    injectJavaScript = (script) => {
        this.wv.injectJavaScript(`
        javascript:(function(){${script} })()
          `);
        //   setTimeout(function() { ${script} });
    }

    setAnalyticsData = async () => {
        const uuid = await UserInfo.getBootpayUUID();
        const bootpaySK = await UserInfo.getBootpaySK();
        const bootLastTime = await UserInfo.getBootpayLastTime();

        console.log('bootLastTime', bootLastTime, Date.now(), '---', Date.now() - bootLastTime);

        const elaspedTime = Date.now() - bootLastTime;
        this.injectJavaScript(` 
        window.BootPay.setAnalyticsData({uuid:'${uuid}',sk:'${bootpaySK}',sk_time:${bootLastTime},time:${elaspedTime}});
        `); 
    }
}

// BootpayWebView.prototype = 