//package org.example.project
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.snapshots.SnapshotStateList
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Window
//import androidx.compose.ui.window.application
//import java.io.File
//import java.net.URLClassLoader
//import java.util.jar.JarFile
//import kotlin.reflect.full.createInstance
//import kotlin.reflect.full.findAnnotation
//
//// Цветовая схема
//val BackgroundColor = Color(0xFF1E1E1E)
//val PrimaryColor = Color(0xFF6200EE)
//val SecondaryColor = Color(0xFF03DAC6)
//val SurfaceColor = Color(0xFF2D2D2D)
//val ErrorColor = Color(0xFFCF6679)
//val SuccessColor = Color(0xFF4CAF50)
//val TextColor = Color.White
//val BorderColor = Color(0xFF4A4A4A)
//
//// Интерфейсы для плагинов
//interface PluginModule {
//  val name: String
//  val description: String
//  val converters: List<PluginConverter<*, *>>
//}
//
//interface PluginConverter<T : Any, H : Any> {
//  val name: String
//  val description: String
//  val inputType: Class<T>
//  val outputType: Class<H>
//  fun convert(input: Any): Any
//}
//
//// Аннотации для плагинов
//@Target(AnnotationTarget.CLASS)
//annotation class PluginModuleInfo(val name: String, val description: String)
//
//@Target(AnnotationTarget.FUNCTION)
//annotation class PluginConverterInfo(val name: String, val description: String)
//
//// Состояние приложения
//data class BundleItem(
//  val converter: PluginConverter<*, *>,
//  val moduleName: String,
//  val converterName: String
//)
//
//data class BundleConfig(
//  val name: String,
//  val items: MutableList<BundleItem> = mutableListOf()
//)
//
//// Основное приложение
//@OptIn(ExperimentalMaterial3Api::class)
//fun main() = application {
//  val scope = rememberCoroutineScope()
//  var pluginManager by remember { mutableStateOf(PluginManager()) }
//  var bundles by remember { mutableStateOf(mutableStateListOf<BundleConfig>()) }
//  var currentBundle by remember { mutableStateOf<BundleConfig?>(null) }
//  var selectedConverter by remember { mutableStateOf<PluginConverter<*, *>?>(null) }
//
//  LaunchedEffect(Unit) {
//    pluginManager.loadPlugins()
//  }
//
//  Window(
//    onCloseRequest = { exitApplication() },
//    title = "Plugin Bundle Creator"
//  ) {
//    Scaffold(
//      topBar = {
//        TopAppBar(
//          title = { Text("Plugin Bundle Creator", color = TextColor) },
//          colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = SurfaceColor,
//            titleContentColor = TextColor
//          )
//        )
//      },
//      containerColor = BackgroundColor
//    ) { padding ->
//      Row(
//        modifier = Modifier
//          .fillMaxSize()
//          .padding(padding)
//      ) {
//        // Панель плагинов
//        PluginsPanel(
//          pluginManager = pluginManager,
//          selectedConverter = selectedConverter,
//          onConverterSelected = { converter -> selectedConverter = converter }
//        )
//
//        // Разделитель
//        Divider(
//          modifier = Modifier
//            .fillMaxHeight()
//            .width(2.dp)
//            .padding(vertical = 16.dp),
//          color = BorderColor
//        )
//
//        // Панель связок
//        BundlesPanel(
//          bundles = bundles,
//          currentBundle = currentBundle,
//          onBundleSelected = { bundle -> currentBundle = bundle },
//          onBundleCreated = { name ->
//            val newBundle = BundleConfig(name)
//            bundles.add(newBundle)
//            currentBundle = newBundle
//          },
//          onBundleDeleted = { bundle ->
//            val index = bundles.indexOf(bundle)
//            if (index != -1) {
//              bundles.removeAt(index)
//              if (currentBundle == bundle) {
//                currentBundle = if (bundles.isNotEmpty()) bundles.last() else null
//              }
//            }
//          }
//        )
//
//        // Панель редактирования связки
//        BundleEditorPanel(
//          currentBundle = currentBundle,
//          selectedConverter = selectedConverter,
//          onAddConverter = { converter ->
//            currentBundle?.items?.add(
//              BundleItem(
//                converter = converter,
//                moduleName = pluginManager.getModuleName(converter),
//                converterName = converter.name
//              )
//            )
//          },
//          onRemoveConverter = { index ->
//            currentBundle?.items?.removeAt(index)
//          },
//          onExecuteBundle = { input ->
//            if (currentBundle != null && currentBundle!!.items.isNotEmpty()) {
//              try {
//                var result: Any = input
//                for (item in currentBundle!!.items) {
//                  result = item.converter.convert(result)
//                }
//                return@BundleEditorPanel "Result: $result"
//              } catch (e: Exception) {
//                return@BundleEditorPanel "Error: ${e.message}"
//              }
//            }
//            return@BundleEditorPanel "No converters in bundle"
//          }
//        )
//      }
//    }
//  }
//}
//
//@Composable
//fun PluginsPanel(
//  pluginManager: PluginManager,
//  selectedConverter: PluginConverter<*, *>?,
//  onConverterSelected: (PluginConverter<*, *>) -> Unit
//) {
//  Column(
//    modifier = Modifier
//      .width(300.dp)
//      .fillMaxHeight()
//      .background(SurfaceColor)
//      .padding(16.dp)
//  ) {
//    Text(
//      text = "Available Plugins",
//      style = MaterialTheme.typography.titleMedium,
//      color = TextColor,
//      fontWeight = FontWeight.Bold
//    )
//
//    Spacer(modifier = Modifier.height(16.dp))
//
//    if (pluginManager.isLoading) {
//      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        CircularProgressIndicator(color = PrimaryColor)
//      }
//    } else if (pluginManager.modules.isEmpty()) {
//      Text(
//        text = "No plugins found. Place JAR files in ${pluginManager.pluginsFolder.absolutePath}",
//        color = TextColor,
//        modifier = Modifier.padding(8.dp)
//      )
//    } else {
//      LazyColumn {
//        items(pluginManager.modules) { module ->
//          ModuleCard(
//            module = module,
//            isSelected = selectedConverter?.let {
//              module.converters.any { converter -> converter == it }
//            } == true,
//            onConverterClick = onConverterSelected
//          )
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun ModuleCard(
//  module: PluginModule,
//  isSelected: Boolean,
//  onConverterClick: (PluginConverter<*, *>) -> Unit
//) {
//  Card(
//    modifier = Modifier
//      .fillMaxWidth()
//      .padding(vertical = 4.dp),
//    colors = CardDefaults.cardColors(
//      containerColor = if (isSelected) PrimaryColor.copy(alpha = 0.2f) else SurfaceColor
//    )
//  ) {
//    Column(modifier = Modifier.padding(12.dp)) {
//      Text(
//        text = module.name,
//        style = MaterialTheme.typography.titleSmall,
//        color = PrimaryColor,
//        fontWeight = FontWeight.Bold
//      )
//
//      Text(
//        text = module.description,
//        style = MaterialTheme.typography.bodySmall,
//        color = TextColor.copy(alpha = 0.7f),
//        modifier = Modifier.padding(top = 2.dp)
//      )
//
//      Spacer(modifier = Modifier.height(8.dp))
//
//      LazyColumn {
//        items(module.converters) { converter ->
//          ConverterItem(
//            converter = converter,
//            onClick = { onConverterClick(converter) }
//          )
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun ConverterItem(
//  converter: PluginConverter<*, *>,
//  onClick: () -> Unit
//) {
//  Box(
//    modifier = Modifier
//      .fillMaxWidth()
//      .padding(vertical = 4.dp)
//      .clip(RoundedCornerShape(4.dp))
//      .background(SurfaceColor.copy(alpha = 0.7f))
//      .clickable(onClick = onClick)
//      .padding(8.dp)
//  ) {
//    Column {
//      Text(
//        text = converter.name,
//        style = MaterialTheme.typography.bodyMedium,
//        color = SecondaryColor,
//        fontWeight = FontWeight.Medium
//      )
//
//      Text(
//        text = "${converter.inputType.simpleName} → ${converter.outputType.simpleName}",
//        style = MaterialTheme.typography.bodySmall,
//        color = TextColor.copy(alpha = 0.6f)
//      )
//
//      Text(
//        text = converter.description,
//        style = MaterialTheme.typography.bodySmall,
//        color = TextColor.copy(alpha = 0.8f),
//        modifier = Modifier.padding(top = 2.dp)
//      )
//    }
//  }
//}
//
//@Composable
//fun BundlesPanel(
//  bundles: SnapshotStateList<BundleConfig>,
//  currentBundle: BundleConfig?,
//  onBundleSelected: (BundleConfig) -> Unit,
//  onBundleCreated: (String) -> Unit,
//  onBundleDeleted: (BundleConfig) -> Unit
//) {
//  Column(
//    modifier = Modifier
//      .width(250.dp)
//      .fillMaxHeight()
//      .background(SurfaceColor)
//      .padding(16.dp)
//  ) {
//    Row(
//      modifier = Modifier.fillMaxWidth(),
//      verticalAlignment = Alignment.CenterVertically
//    ) {
//      Text(
//        text = "Bundles",
//        style = MaterialTheme.typography.titleMedium,
//        color = TextColor,
//        fontWeight = FontWeight.Bold,
//        modifier = Modifier.weight(1f)
//      )
//
//      Button(
//        onClick = {
//          val name = "Bundle ${bundles.size + 1}"
//          onBundleCreated(name)
//        },
//        colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor.copy(alpha = 0.3f))
//      ) {
//        Text("+", color = SecondaryColor, fontWeight = FontWeight.Bold)
//      }
//    }
//
//    Spacer(modifier = Modifier.height(16.dp))
//
//    if (bundles.isEmpty()) {
//      Text(
//        text = "No bundles created. Click + to create one.",
//        color = TextColor.copy(alpha = 0.7f),
//        modifier = Modifier.padding(8.dp)
//      )
//    } else {
//      LazyColumn {
//        items(bundles) { bundle ->
//          BundleCard(
//            bundle = bundle,
//            isSelected = currentBundle == bundle,
//            onDelete = { onBundleDeleted(bundle) },
//            onClick = { onBundleSelected(bundle) }
//          )
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun BundleCard(
//  bundle: BundleConfig,
//  isSelected: Boolean,
//  onDelete: () -> Unit,
//  onClick: () -> Unit
//) {
//  Card(
//    modifier = Modifier
//      .fillMaxWidth()
//      .padding(vertical = 4.dp)
//      .clickable(onClick = onClick),
//    colors = CardDefaults.cardColors(
//      containerColor = if (isSelected) PrimaryColor.copy(alpha = 0.3f) else SurfaceColor
//    )
//  ) {
//    Row(
//      modifier = Modifier
//        .fillMaxWidth()
//        .padding(12.dp),
//      verticalAlignment = Alignment.CenterVertically
//    ) {
//      Column(modifier = Modifier.weight(1f)) {
//        Text(
//          text = bundle.name,
//          style = MaterialTheme.typography.bodyMedium,
//          color = if (isSelected) PrimaryColor else TextColor,
//          fontWeight = FontWeight.Bold
//        )
//
//        Text(
//          text = "${bundle.items.size} converters",
//          style = MaterialTheme.typography.bodySmall,
//          color = TextColor.copy(alpha = 0.7f)
//        )
//      }
//
//      Button(
//        onClick = onDelete,
//        colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.3f)),
//        modifier = Modifier.size(24.dp)
//      ) {
//        Text("X", color = ErrorColor, fontWeight = FontWeight.Bold, modifier = Modifier.size(12.dp))
//      }
//    }
//  }
//}
//
//@Composable
//fun BundleEditorPanel(
//  currentBundle: BundleConfig?,
//  selectedConverter: PluginConverter<*, *>?,
//  onAddConverter: (PluginConverter<*, *>) -> Unit,
//  onRemoveConverter: (Int) -> Unit,
//  onExecuteBundle: (String) -> String
//) {
//  Column(
//    modifier = Modifier
//      .fillMaxHeight()
////      .weight(1f)
//      .background(SurfaceColor)
//      .padding(16.dp)
//  ) {
//    if (currentBundle == null) {
//      Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//      ) {
//        Text(
//          text = "Select or create a bundle to edit",
//          color = TextColor.copy(alpha = 0.7f),
//          style = MaterialTheme.typography.bodyLarge
//        )
//      }
//    } else {
//      Text(
//        text = "Editing: ${currentBundle.name}",
//        style = MaterialTheme.typography.titleMedium,
//        color = PrimaryColor,
//        fontWeight = FontWeight.Bold
//      )
//
//      Spacer(modifier = Modifier.height(16.dp))
//
//      // Добавление конвертера
//      if (selectedConverter != null) {
//        Button(
//          onClick = { onAddConverter(selectedConverter) },
//          modifier = Modifier.fillMaxWidth(),
//          colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
//        ) {
//          Text("Add: ${selectedConverter.name}", color = Color.White)
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//      }
//
//      // Список конвертеров в связке
//      if (currentBundle.items.isEmpty()) {
//        Box(
//          modifier = Modifier.fillMaxSize(0.5f),
//          contentAlignment = Alignment.Center
//        ) {
//          Text(
//            text = "No converters in bundle. Select a converter from the left panel and click 'Add'.",
//            color = TextColor.copy(alpha = 0.7f),
//            textAlign = TextAlign.Center
//          )
//        }
//      } else {
//        Text(
//          text = "Bundle Converters (${currentBundle.items.size})",
//          style = MaterialTheme.typography.bodyMedium,
//          color = TextColor,
//          fontWeight = FontWeight.Bold,
//          modifier = Modifier.padding(vertical = 8.dp)
//        )
//
//        LazyColumn(modifier = Modifier.weight(1f)) {
//          itemsIndexed(currentBundle.items.toList()) { index, item ->
//            BundleItemCard(
//              item = item,
//              index = index,
//              onRemove = { onRemoveConverter(index) }
//            )
//          }
//        }
//      }
//
//      // Тест выполнения
//      if (currentBundle.items.isNotEmpty()) {
//        Spacer(modifier = Modifier.height(16.dp))
//
//        var testInput by remember { mutableStateOf("42") }
//        var testResult by remember { mutableStateOf<String?>(null) }
//
//        OutlinedTextField(
//          value = testInput,
//          onValueChange = { testInput = it },
//          label = { Text("Test Input", color = TextColor) },
//          modifier = Modifier.fillMaxWidth(),
//          colors = OutlinedTextFieldDefaults.colors(
//            focusedTextColor = TextColor,
//            unfocusedTextColor = TextColor.copy(alpha = 0.8f),
//            focusedContainerColor = SurfaceColor,
//            unfocusedContainerColor = SurfaceColor,
//            focusedBorderColor = PrimaryColor,
//            unfocusedBorderColor = BorderColor,
//            cursorColor = PrimaryColor
//          )
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Button(
//          onClick = {
//            testResult = onExecuteBundle(testInput)
//          },
//          modifier = Modifier.fillMaxWidth(),
//          colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
//        ) {
//          Text("Execute Bundle", color = Color.White)
//        }
//
//        if (testResult != null) {
//          Spacer(modifier = Modifier.height(8.dp))
//          Text(
//            text = testResult!!,
//            color = if (testResult!!.contains("Error")) ErrorColor else SuccessColor,
//            modifier = Modifier.padding(8.dp)
//          )
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun BundleItemCard(
//  item: BundleItem,
//  index: Int,
//  onRemove: () -> Unit
//) {
//  Card(
//    modifier = Modifier
//      .fillMaxWidth()
//      .padding(vertical = 4.dp),
//    colors = CardDefaults.cardColors(containerColor = SurfaceColor.copy(alpha = 0.8f))
//  ) {
//    Row(
//      modifier = Modifier
//        .fillMaxWidth()
//        .padding(12.dp),
//      verticalAlignment = Alignment.CenterVertically
//    ) {
//      Column(modifier = Modifier.weight(1f)) {
//        Row(verticalAlignment = Alignment.CenterVertically) {
//          Text(
//            text = "#${index + 1}",
//            style = MaterialTheme.typography.bodySmall,
//            color = PrimaryColor,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(end = 8.dp)
//          )
//          Text(
//            text = item.converterName,
//            style = MaterialTheme.typography.bodyMedium,
//            color = SecondaryColor,
//            fontWeight = FontWeight.Medium
//          )
//        }
//
//        Text(
//          text = "${item.moduleName}: ${item.converter.inputType.simpleName} → ${item.converter.outputType.simpleName}",
//          style = MaterialTheme.typography.bodySmall,
//          color = TextColor.copy(alpha = 0.8f),
//          modifier = Modifier.padding(top = 2.dp)
//        )
//      }
//
//      Button(
//        onClick = onRemove,
//        colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.3f)),
//        modifier = Modifier.size(24.dp)
//      ) {
//        Text("-", color = ErrorColor, fontWeight = FontWeight.Bold, modifier = Modifier.size(12.dp))
//      }
//    }
//  }
//}
//
//// Менеджер плагинов
//class PluginManager {
//  val pluginsFolder = File("plugins")
//  private val classLoaders = mutableListOf<URLClassLoader>()
//  val modules = mutableStateListOf<PluginModule>()
//  var isLoading by mutableStateOf(true)
//
//  init {
//    pluginsFolder.mkdirs()
//  }
//
//  suspend fun loadPlugins() {
//    isLoading = true
//    modules.clear()
//    classLoaders.forEach { it.close() }
//    classLoaders.clear()
//
//    try {
//      loadPluginsFromFolder(pluginsFolder)
//
//      // Загружаем также из подпапок
//      pluginsFolder.listFiles { file -> file.isDirectory }?.forEach { subfolder ->
//        loadPluginsFromFolder(subfolder)
//      }
//
//      isLoading = false
//    } catch (e: Exception) {
//      e.printStackTrace()
//      isLoading = false
//    }
//  }
//
//  private fun loadPluginsFromFolder(folder: File) {
//    folder.listFiles { file -> file.isFile && file.name.endsWith(".jar") }?.forEach { jarFile ->
//      try {
//        val url = jarFile.toURI().toURL()
//        val classLoader = URLClassLoader(arrayOf(url), this::class.java.classLoader)
//        classLoaders.add(classLoader)
//
//        // Находим все классы в JAR файле
//        JarFile(jarFile).use { jar ->
//          val entries = jar.entries()
//          while (entries.hasMoreElements()) {
//            val entry = entries.nextElement()
//            if (entry.name.endsWith(".class") && !entry.name.contains('$')) {
//              val className = entry.name.replace('/', '.').removeSuffix(".class")
//              try {
//                val clazz = Class.forName(className, true, classLoader)
//                if (PluginModule::class.java.isAssignableFrom(clazz)) {
//                  val moduleClass = clazz.kotlin
//                  val moduleAnnotation = moduleClass.findAnnotation<PluginModuleInfo>()
//
//                  if (moduleAnnotation != null) {
//                    val moduleInstance = moduleClass.objectInstance ?: moduleClass.createInstance()
//                    if (moduleInstance is PluginModule) {
//                      modules.add(moduleInstance)
//                    }
//                  }
//                }
//              } catch (e: Throwable) {
//                // Игнорируем классы, которые не являются модулями
//              }
//            }
//          }
//        }
//      } catch (e: Exception) {
//        e.printStackTrace()
//      }
//    }
//  }
//
//  fun getModuleName(converter: PluginConverter<*, *>): String {
//    return modules.firstOrNull { it.converters.contains(converter) }?.name ?: "Unknown Module"
//  }
//}