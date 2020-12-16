import { Component, ReactNode } from 'react';
import { ViewProperties, EmitterSubscription } from 'react-native';
import { EventEmitter } from 'events';


interface BootpayWebViewProps {
    ios_application_id: string;
    android_application_id: string;

    onCancel: (data: string) => void;
    onError: (data: string) => void;
    onReady: (data: string) => void;
    onConfirm: (data: string) => void;
    onDone: (data: string) => void;
    onClose: () => void;
  }

export class BootpayWebView extends Component<BootpayWebViewProps> { 
    request: (payload: Object, items: Object, user: Object, extra: Object) => Promise<string>;
    dismiss: () => Promise<string>;
    transactionConfirm: (data: string) => Promise<string>;
}

 
export { BootpayWebView }