package org.example.project.api

typealias DataHolderAny = DataHolder<*>

@Suppress("UNCHECKED_CAST")
interface DataHolder<Impl : DataHolder<Impl>> {
  var data: Data
  
  fun addData(key: String, value: Any): Impl {
    data[key] = value
    return this as Impl
  }
  
  fun addData(map: MutableMap<String, Any>): Impl {
    data += map
    return this as Impl
  }
  
  fun addData(vararg pairs: Pair<String, Any>): Impl {
    pairs.forEach { pair ->
      data[pair.first] = pair.second
    }
    
    return this as Impl
  }
  
  fun editData(key: String, value: Any): Impl {
    data[key] = value
    return this as Impl
  }
  
  fun editData(pair: Pair<String, Any>): Impl {
    data[pair.first] = pair.second
    return this as Impl
  }
  
  fun editData(map: Map<String, Any>): Impl {
    map.forEach {
      data[it.key] = it.value
    }
    return this as Impl
  }
  
  fun removeData(key: String): Impl {
    data.remove(key)
    return this as Impl
  }
}