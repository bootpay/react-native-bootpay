//
//  BPCWKProccessPoolManager.h
//  Bootpay
//
//  Created by Taesup Yoon on 2020/12/14.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <WebKit/WebKit.h>

@interface BPCWKProccessPoolManager : NSObject

+ (instancetype) sharedManager;
- (WKProcessPool *)sharedProcessPool;

@end
