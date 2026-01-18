package ru_lonya.util.codec

interface AnyCodec<Object : Any?, Data : Any?> {
	fun encode(obj: Object): Data
	fun decode(data: Data) : Object
}

interface AnyCodecHolder<Codec : AnyCodec<Object, Data>, Object : Any?, Data : Any?> {
	fun codec(): Codec
}

inline fun <Object : Any?, Data : Any?> codec(
	crossinline encoder: (Object) -> Data,
	crossinline decoder: (Data) -> Object
) = object : AnyCodec<Object, Data> {
	override fun encode(obj: Object): Data = encoder(obj)
	override fun decode(data: Data): Object = decoder(data)
}

inline fun <Object : Any?, Data : Any?> codecThis(
	crossinline encoder: Object.() -> Data,
	crossinline decoder: Data.() -> Object
) = object : AnyCodec<Object, Data> {
	override fun encode(obj: Object): Data = obj.encoder()
	override fun decode(data: Data): Object = data.decoder()
}