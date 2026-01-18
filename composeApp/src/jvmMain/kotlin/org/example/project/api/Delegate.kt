package org.example.project.api

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

typealias ReadOnlyDelegateProvider<T, O> = PropertyDelegateProvider<T, ReadOnlyProperty<T, O>>
typealias ReadWriteDelegateProvider<T, O> = PropertyDelegateProvider<T, ReadWriteProperty<T, O>>