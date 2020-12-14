import { NativeModules } from 'react-native';

type BootpayType = {
  multiply(a: number, b: number): Promise<number>;
};

const { Bootpay } = NativeModules;

export default Bootpay as BootpayType;
