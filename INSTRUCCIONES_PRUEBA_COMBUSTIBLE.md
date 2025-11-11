# 🚀 Instrucciones de Compilación y Prueba - Formateo de Combustible

## 📋 Resumen Ejecutivo

Se ha implementado un sistema de formateo automático de números para los campos de combustible en la aplicación Android. Este documento contiene las instrucciones para compilar, instalar y probar la nueva funcionalidad.

---

## ✅ Cambios Realizados

### Archivo Modificado
- ✅ `app/src/main/java/com/uvrp/itsmantenimientoapp/RegistrarCombustibleActivity.kt`

### Documentación Creada
- ✅ `FORMATEO_NUMEROS_COMBUSTIBLE.md` - Documentación técnica completa
- ✅ `RESUMEN_CAMBIOS_COMBUSTIBLE.md` - Resumen de cambios
- ✅ `EJEMPLOS_VISUALES_FORMATEO.md` - Guía visual con ejemplos
- ✅ `INSTRUCCIONES_PRUEBA_COMBUSTIBLE.md` - Este archivo

---

## 🔧 Paso 1: Compilar la Aplicación

### Opción A: Desde Android Studio

1. **Abrir el proyecto**
   - Abrir Android Studio
   - File → Open
   - Seleccionar: `C:\Users\RaulHenao\StudioProjects\appmantenimientoits`

2. **Sincronizar Gradle**
   - Esperar a que Gradle sincronice automáticamente
   - O manualmente: File → Sync Project with Gradle Files

3. **Compilar**
   - Build → Make Project
   - O usar el atajo: `Ctrl + F9`

4. **Generar APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Esperar a que termine la compilación
   - El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

### Opción B: Desde Línea de Comandos (PowerShell)

```powershell
# Navegar al directorio del proyecto
cd C:\Users\RaulHenao\StudioProjects\appmantenimientoits

# Compilar el proyecto
.\gradlew.bat assembleDebug

# El APK estará en: app\build\outputs\apk\debug\app-debug.apk
```

### Opción C: Generar APK Release (Producción)

```powershell
# Navegar al directorio del proyecto
cd C:\Users\RaulHenao\StudioProjects\appmantenimientoits

# Compilar versión release
.\gradlew.bat assembleRelease

# El APK estará en: app\build\outputs\apk\release\app-release.apk
```

---

## 📱 Paso 2: Instalar en Dispositivo

### Opción A: Desde Android Studio

1. **Conectar dispositivo Android**
   - Conectar el dispositivo por USB
   - Activar "Depuración USB" en el dispositivo
   - Verificar que Android Studio detecte el dispositivo

2. **Instalar y ejecutar**
   - Seleccionar el dispositivo en la barra superior
   - Presionar el botón "Run" (▶️)
   - O usar el atajo: `Shift + F10`

### Opción B: Desde Línea de Comandos

```powershell
# Verificar que el dispositivo esté conectado
adb devices

# Instalar el APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# O si ya está instalado, reinstalar
adb uninstall com.uvrp.itsmantenimientoapp
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Opción C: Instalación Manual

1. Copiar el APK al dispositivo (USB, email, Drive, etc.)
2. En el dispositivo, abrir el archivo APK
3. Permitir instalación de fuentes desconocidas si es necesario
4. Instalar la aplicación

---

## 🧪 Paso 3: Realizar Pruebas

### Prueba 1: Formateo Básico de Miles

**Objetivo**: Verificar que los números se formateen automáticamente con puntos de miles.

**Pasos**:
1. Abrir la aplicación
2. Navegar a "Registrar Combustible"
3. Seleccionar un vehículo
4. En el campo "Cantidad (GL)", escribir: `15000`
5. **Resultado esperado**: Debe mostrar `15.000`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 2: Decimales con Coma

**Objetivo**: Verificar que se puedan escribir decimales con coma.

**Pasos**:
1. En el campo "Valor por Galón", escribir: `16400`
2. Debe mostrar: `16.400`
3. Escribir: `,`
4. Debe mostrar: `16.400,`
5. Escribir: `5`
6. Debe mostrar: `16.400,5`
7. Escribir: `0`
8. **Resultado esperado**: Debe mostrar `16.400,50`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 3: Borrado Dinámico

**Objetivo**: Verificar que el formateo se actualice al borrar dígitos.

**Pasos**:
1. En el campo "Valor Total", escribir: `15000`
2. Debe mostrar: `15.000`
3. Presionar backspace (borrar un 0)
4. Debe mostrar: `1.500`
5. Presionar backspace (borrar otro 0)
6. **Resultado esperado**: Debe mostrar `150`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 4: Edición en el Medio

**Objetivo**: Verificar que se pueda editar en cualquier posición.

**Pasos**:
1. En el campo "Cantidad", escribir: `15000`
2. Debe mostrar: `15.000`
3. Tocar entre el `1` y el `5` para posicionar el cursor
4. Escribir: `2`
5. **Resultado esperado**: Debe mostrar `125.000`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 5: Límite de Decimales

**Objetivo**: Verificar que solo se permitan 2 decimales.

**Pasos**:
1. En el campo "Valor por Galón", escribir: `100,123456789`
2. **Resultado esperado**: Debe mostrar solo `100,12`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 6: Números Grandes

**Objetivo**: Verificar el formateo de números muy grandes.

**Pasos**:
1. En el campo "Kilometraje Inicial", escribir: `1234567`
2. **Resultado esperado**: Debe mostrar `1.234.567`
3. Escribir: `,89`
4. **Resultado esperado**: Debe mostrar `1.234.567,89`

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 7: Guardar Registro Completo

**Objetivo**: Verificar que el registro se guarde correctamente.

**Pasos**:
1. Llenar todos los campos:
   - Kilometraje Inicial: `124567,89`
   - Cantidad: `15,5`
   - Valor por Galón: `16400,00`
   - Valor Total: `253700,00`
2. Tomar foto del ticket
3. Agregar observación (opcional)
4. Presionar "Registrar Tanqueo"
5. Confirmar el registro
6. **Resultado esperado**: 
   - Mensaje de éxito
   - Volver a la pantalla anterior
   - Registro guardado en BD local

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 8: Sincronización con Servidor

**Objetivo**: Verificar que los datos se sincronicen correctamente.

**Pasos**:
1. Registrar un combustible con los valores de la Prueba 7
2. Ir a la pantalla principal (Home)
3. Verificar que aparezca en "Combustibles Pendientes"
4. Presionar el botón de sincronización
5. **Resultado esperado**:
   - Sincronización exitosa
   - Combustible marcado como sincronizado
   - Valores correctos en el servidor

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 9: Persistencia de Datos

**Objetivo**: Verificar que los datos se guarden temporalmente.

**Pasos**:
1. Llenar el campo "Cantidad": `15000`
2. Llenar el campo "Valor por Galón": `16400,50`
3. Salir de la pantalla (presionar atrás)
4. Volver a entrar a "Registrar Combustible"
5. **Resultado esperado**: Los valores deben estar guardados

**Estado**: ☐ Pasó ☐ Falló

---

### Prueba 10: Validación de Campos Vacíos

**Objetivo**: Verificar que se validen los campos obligatorios.

**Pasos**:
1. Dejar todos los campos vacíos
2. Presionar "Registrar Tanqueo"
3. **Resultado esperado**: 
   - Error en el primer campo vacío
   - Mensaje: "Campo obligatorio"
   - No se permite guardar

**Estado**: ☐ Pasó ☐ Falló

---

## 📊 Resumen de Pruebas

```
Total de Pruebas: 10
Pasadas: ___
Falladas: ___
Pendientes: ___

Porcentaje de éxito: ____%
```

---

## 🐛 Registro de Problemas Encontrados

### Problema 1
**Descripción**: 
**Pasos para reproducir**: 
**Resultado esperado**: 
**Resultado actual**: 
**Prioridad**: ☐ Alta ☐ Media ☐ Baja

### Problema 2
**Descripción**: 
**Pasos para reproducir**: 
**Resultado esperado**: 
**Resultado actual**: 
**Prioridad**: ☐ Alta ☐ Media ☐ Baja

---

## 🔍 Verificación en Base de Datos

### Verificar Valores Guardados

```sql
-- Conectar a la base de datos SQLite del dispositivo
adb shell
cd /data/data/com.uvrp.itsmantenimientoapp/databases/
sqlite3 mantenimiento.db

-- Ver los combustibles registrados
SELECT 
    id,
    cantidad_galones,
    valor_galon,
    valor_total,
    kilometraje_inicial,
    fecha_tanqueo
FROM combustible
ORDER BY id DESC
LIMIT 5;

-- Salir
.exit
exit
```

**Valores esperados**:
- `cantidad_galones`: 15.5 (no 15.000,50)
- `valor_galon`: 16400.0 (no 16.400,00)
- `valor_total`: 253700.0 (no 253.700,00)
- `kilometraje_inicial`: 124567.89 (no 124.567,89)

---

## 📱 Verificación en Diferentes Dispositivos

### Dispositivo 1
**Modelo**: _________________
**Android**: _________________
**Resolución**: _________________
**Resultado**: ☐ OK ☐ Problemas

### Dispositivo 2
**Modelo**: _________________
**Android**: _________________
**Resolución**: _________________
**Resultado**: ☐ OK ☐ Problemas

### Dispositivo 3
**Modelo**: _________________
**Android**: _________________
**Resolución**: _________________
**Resultado**: ☐ OK ☐ Problemas

---

## 🔧 Solución de Problemas Comunes

### Problema: "No compila el proyecto"

**Solución**:
```powershell
# Limpiar el proyecto
.\gradlew.bat clean

# Volver a compilar
.\gradlew.bat assembleDebug
```

### Problema: "El dispositivo no aparece en Android Studio"

**Solución**:
1. Verificar que la depuración USB esté activada
2. Verificar que los drivers estén instalados
3. Probar con otro cable USB
4. Ejecutar: `adb kill-server` y luego `adb start-server`

### Problema: "Error al instalar el APK"

**Solución**:
```powershell
# Desinstalar la versión anterior
adb uninstall com.uvrp.itsmantenimientoapp

# Reinstalar
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Problema: "La aplicación se cierra al abrir Registrar Combustible"

**Solución**:
1. Verificar los logs:
```powershell
adb logcat | findstr "RegistrarCombustible"
```
2. Buscar errores en los logs
3. Verificar que Firebase esté configurado correctamente

---

## 📝 Checklist Final

Antes de considerar la funcionalidad como completa, verificar:

- ☐ Todas las pruebas pasaron exitosamente
- ☐ No hay errores de compilación
- ☐ No hay warnings críticos
- ☐ La aplicación no se cierra inesperadamente
- ☐ El formateo funciona en todos los campos
- ☐ Los valores se guardan correctamente en BD
- ☐ La sincronización funciona correctamente
- ☐ La persistencia temporal funciona
- ☐ Las validaciones funcionan correctamente
- ☐ Probado en al menos 2 dispositivos diferentes
- ☐ Documentación completa y actualizada

---

## 📞 Contacto y Soporte

Si encuentras problemas o tienes preguntas:

1. **Revisar la documentación**:
   - `FORMATEO_NUMEROS_COMBUSTIBLE.md`
   - `EJEMPLOS_VISUALES_FORMATEO.md`
   - `RESUMEN_CAMBIOS_COMBUSTIBLE.md`

2. **Verificar logs**:
   ```powershell
   adb logcat -s "RegistrarCombustible"
   ```

3. **Revisar el código**:
   - Archivo: `RegistrarCombustibleActivity.kt`
   - Función: `configurarFormatoNumericoConMiles()`
   - Función: `calcularPosicionCursor()`

---

## 🎯 Próximos Pasos Después de las Pruebas

1. **Si todas las pruebas pasan**:
   - ✅ Marcar la funcionalidad como completa
   - ✅ Generar APK de producción
   - ✅ Distribuir a usuarios finales
   - ✅ Monitorear feedback

2. **Si hay problemas**:
   - 🔧 Documentar los problemas encontrados
   - 🔧 Priorizar según criticidad
   - 🔧 Corregir los problemas
   - 🔧 Repetir las pruebas

---

**Fecha de Creación**: Noviembre 8, 2025  
**Versión**: 1.0  
**Estado**: ✅ Listo para Pruebas

---

## 📋 Notas Adicionales

- Los cambios son retrocompatibles
- No se requieren cambios en el backend
- No se requieren cambios en la base de datos
- La funcionalidad es opcional (no rompe flujos existentes)
- Se puede revertir fácilmente si es necesario

---

**¡Buena suerte con las pruebas! 🚀**

