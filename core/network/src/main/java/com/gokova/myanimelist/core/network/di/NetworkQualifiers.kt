package com.gokova.myanimelist.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Unauthenticated

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authenticated
