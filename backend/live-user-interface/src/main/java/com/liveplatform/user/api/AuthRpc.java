package com.liveplatform.user.api;

/** 登录与会话契约。验证码为字符串，保留前导零；身份只从服务端 Token 映射获取。 */
public interface AuthRpc {
    LoginCodeDTO sendCode(String phone) throws UserBusinessException;
    LoginResultDTO login(String phone, String code) throws UserBusinessException;
    UserDTO currentUser(String token) throws UserBusinessException;
    void logout(String token) throws UserBusinessException;
}
