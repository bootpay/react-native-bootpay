import React, { Component } from 'react';
import DeviceInfo from 'react-native-device-info';
import SInfo from 'react-native-sensitive-info'; 

export default class UserInfo extends Component { 
    static getBootpayInfo = (key, defaultVal) => {
        return new Promise((resolve, reject) => {
            SInfo.getItem(key, {
                sharedPreferencesName: 'bootpaySharedPrefs',
                keychainService: 'bootpayKeychain'
            }).then((res) => { 
                res == undefined ? resolve(defaultVal) : resolve(res); 
                resolve(res);
            }).catch((error) => { 
                reject(error);
            }); 
        })
    }

    static setBootpayInfo = (key, val) => {
        return new Promise((resolve, reject) => {
            SInfo.setItem(String(key), String(val), {
                sharedPreferencesName: 'bootpaySharedPrefs',
                keychainService: 'bootpayKeychain'
            }).then((res) => { 
                resolve(res);
            }).catch((error) => { 
                reject(error);
            }); 
        })
    }

    static getBootpayUUID = () => {
        let uuid = DeviceInfo.getUniqueId(); 
        console.log("uuid1: " + uuid);
        return this.setBootpayInfo('uuid', uuid);  
    }

    static getBootpaySK = () => {
        return this.getBootpayInfo('bootpay_sk', ''); 
    }

    static setBootpaySK = (val) => {
        return this.setBootpayInfo('bootpay_sk', val); 
    }

    static newBootpaySK = (uuid, time) => {
        console.log("uuid3: " + uuid);
        return this.setBootpaySK(`${uuid}_${time}`); 
    }

    static getBootpayLastTime = async () => {
        const time = await this.getBootpayInfo('bootpay_last_time', 0); 
        return parseInt(time);
    }

    static setBootpayLastTime = (val) => {
        return this.setBootpayInfo('bootpay_last_time', val); 
    }

    static getBootpayUserId = () => {
        return this.getBootpayInfo('bootpay_user_id', ''); 
    }

    static setBootpayUserId = (val) => {
        return this.setBootpayInfo('bootpay_user_id', val); 
    }

    static updateInfo = async () => {
        const uuid = await UserInfo.getBootpayUUID();  
        const bootpaySK = await this.getBootpaySK();
        const lastTime = await this.getBootpayLastTime();
 
        if(bootpaySK == '') await this.newBootpaySK(uuid, current);

        const current = Date.now();
        const isExpired = current - lastTime > 30 * 60 * 1000;
        if(isExpired) this.newBootpaySK(uuid, current);
        this.setBootpayLastTime(current);
    }
}