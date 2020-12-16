//
//  BPCWKProccessPoolManager.m
//  Bootpay
//
//  Created by Taesup Yoon on 2020/12/14.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "BPCWKProccessPoolManager.h"

@interface BPCWKProccessPoolManager() {
    WKProcessPool *_sharedProcessPool;
}
@end

@implementation BPCWKProccessPoolManager

+ (id) sharedManager {
    static BPCWKProccessPoolManager *_sharedManager = nil;
    @synchronized(self) {
        if(_sharedManager == nil) {
            _sharedManager = [[super alloc] init];
        }
        return _sharedManager;
    }
}

- (WKProcessPool *)sharedProcessPool {
    if (!_sharedProcessPool) {
        _sharedProcessPool = [[WKProcessPool alloc] init];
    }
    return _sharedProcessPool;
}

@end
