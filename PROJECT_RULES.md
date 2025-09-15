# Reglas del Proyecto

## Descripción del Proyecto
Proyecto de desarrollo de UI para una aplicación de menú de comida, utilizando Kotlin Multiplatform. Actualmente en fase de pruebas de UI con Jetpack Compose, que servirá como base para la implementación multiplataforma.

## Tecnologías y Frameworks
- **Jetpack Compose**: UI declarativa
- **Koin**: Inyección de dependencias
- **Coil**: Carga y caché de imágenes
- **Ktor**: Cliente HTTP
- **Navigation Compose**: Navegación entre pantallas
- **Room**: Base de datos local
- **Material3**: Sistema de diseño
- **Kotlin Multiplatform**: Base para desarrollo multiplataforma

## 1. Estructura del Proyecto
- [ ] Definir estructura de carpetas
  - `ui/`: Componentes y pantallas
  - `data/`: Repositorios y fuentes de datos
  - `domain/`: Casos de uso y modelos
  - `common/`: Código compartido multiplataforma
- [ ] Establecer convenciones de nombrado
- [ ] Configurar módulos

## 2. Estilo de Código
- [ ] Convenciones de Kotlin
- [ ] Formato de código
- [ ] Reglas de documentación

## 3. Arquitectura
- [ ] Patrón de diseño principal
  - Clean Architecture
  - MVVM para la capa de UI
- [ ] Manejo de estado
  - StateFlow para estado observable
- [ ] Gestión de dependencias
  - Koin para inyección

## 4. Pruebas
- [ ] Tipos de pruebas requeridas
  - Pruebas de UI con Compose
  - Pruebas de navegación
- [ ] Cobertura mínima
- [ ] Herramientas de testing

## 5. Control de Versiones
- [ ] Estrategia de branching
- [ ] Convenciones de commits
- [ ] Proceso de code review

## 6. Seguridad
- [ ] Manejo de datos sensibles
- [ ] Configuraciones de red
- [ ] Permisos de la aplicación

## 7. Rendimiento
- [ ] Objetivos de rendimiento
  - Tiempo de carga de imágenes optimizado
  - Navegación fluida entre pantallas
- [ ] Monitoreo
- [ ] Optimizaciones requeridas

## 8. Documentación
- [ ] Nivel de documentación requerido
- [ ] Herramientas de documentación
- [ ] Proceso de actualización

## Pantallas Actuales
### MenuScreen
- Diseño de dos columnas
- Valoración con estrellas
- Imagen del plato
- Texto descriptivo
- Mejoras propuestas:
  - Implementar animaciones suaves
  - Añadir efectos de hover
  - Mejorar espaciado y tipografía
  - Considerar grid adaptativo

### DetallesScreen
- Imagen central
- Texto justificado a la izquierda
- Descripción con separador
- Mejoras propuestas:
  - Implementar parallax en la imagen
  - Añadir transiciones suaves
  - Mejorar jerarquía visual
  - Considerar scroll personalizado

## Notas
Este documento es dinámico y se actualizará según las necesidades del proyecto.
Cada sección se puede expandir con reglas específicas según se vayan definiendo. 