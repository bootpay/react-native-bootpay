// import React, { useRef }  from 'react';
// import { StyleSheet, View, Text, TouchableOpacity, } from 'react-native';
// import { BootpayWebView } from 'react-native-bootpay';

// export default function App() {
//   const bootpay = useRef<BootpayWebView>(null);

  

//   const onPress = () => {  

//     // let payload = new Payload();
//     const payload = {
//       pg: 'danal',
//       name: '마스카라',
//       order_id: '1234_1234',
//       method: 'card',
//       price: 1000
//     }
//     // payload.bind(payloadData);

//     const items = [
//       {
//         item_name: '키보드',
//         qty: 1,
//         unique: 'ITEM_CODE_KEYBOARD',
//         price: 1000,
//         cat1: '패션',
//         cat2: '여성상의',
//         cat3: '블라우스',
//       }
//     ]

//     const user = {
//       id: '',
//       username: '',
//       user_id: '',
//       email: 'user1234@gmail.com',
//       gender: 0,
//       birth: '',
//       phone: '01012345678',
//       area: '서울',
//       addr: '서울시 동작구 상도로'
//     }



//     const extra = {
//       app_scheme: "bootpaysample", 
//       expire_month: "0", 
//       vbank_result: true, 
//       start_at: "", 
//       end_at: "", 
//       quota: "0,2,3", 
//       offer_period: "", 
//       popup: 1, 
//       quick_popup: 0, 
//       locale: "ko", 
//       disp_cash_result: "Y", 
//       escrow: "0", 
//       theme: "purple", 
//       custom_background: "", 
//       custom_font_color: "", 
//       iosCloseButton: false
//     }

//     // payload.price = 1000;
//     // payload.name = "마스카라";
//     // payload.order_id = "1234_1234";
//     // payload.pg = 'KCP';
//     // payload.method = 'card';
//     // payload.show_agree_window = false;
//     // payload.methods

//     // bootpay.current.re

//     if(bootpay != null && bootpay.current != null) bootpay.current.request(payload, items, user, extra);
//   }


//   return (
//     <View style={styles.container}>
//         <TouchableOpacity
//           style={styles.button}
//           onPress={onPress}
//         >
//           <Text>Press Here</Text>
//         </TouchableOpacity> 
//         <BootpayWebView  
//           ref={bootpay}
//           ios_application_id={'5b8f6a4d396fa665fdc2b5e9'}
//           android_application_id={'5b8f6a4d396fa665fdc2b5e8'} 
//           onCancel={onCancel}
//           onError={onError}
//           onReady={onReady}
//           onConfirm={onConfirm}
//           onDone={onDone}
//           onClose={onClose}
//         />
 
//     </View>
//   ); 
// }


// const onCancel = (data: string) => {
//   console.log('cancel', data);
// }

// const onError = (data: string) => {
//   console.log('error', data);
// }

// const onReady = (data: string) => {
//   console.log('ready', data);
// }

// const onConfirm = (data: string) => {
//   console.log('confirm', data);
// }

// const onDone = (data: string) => {
//   console.log('done', data);
// }

// const onClose = () => {
//   // thi
// }

// const styles = StyleSheet.create({
//   container: {
//     flex: 1,
//     alignItems: 'center',
//     justifyContent: 'center',
//   },
//   button: {
//     alignItems: "center",
//     backgroundColor: "#DDDDDD",
//     padding: 10
//   },
// });
