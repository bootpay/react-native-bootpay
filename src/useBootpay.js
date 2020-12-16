import { useCallback } from 'react';
import BootpayWebView from './BootpayWebView';

export const useBootpay = () => {
    const bootpay = useRef(new BootpayWebView());
    const [bootpayEvents, setBootpayEvents] = useState([]);

    const request = useCallback((payload, items, user, extra) => {
        return bootpay.current.request(payload, items, user, extra);
    }, []);

    const dismiss = useCallback(() => {
        return bootpay.current.dismiss();
    }, []);

    const transactionConfirm = useCallback((data) => {
        return bootpay.current.transactionConfirm(data);
    }, []);

    return [
        {
          bootpayEvents,
        },
        {
            request,
            transactionConfirm,
            dismiss
        },
      ];
}
