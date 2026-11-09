package com.liveplatform.user.provider.cache;

/** 进程内事务事件；不是持久化消息，崩溃后的补偿在后续阶段实现。 */
public record UserChangedEvent(long userId) { }
