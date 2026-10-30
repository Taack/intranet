---
name: create-taack-app
description: Concise guide for creating and structuring a new Taack application/plugin in the `app/` directory (Gradle scaffolding, `@TaackFieldEnum` domains, UI DSLs, `securityClosure` access control, AsciiDoc editor, `TaackAttachmentService`, i18n with `lang=test`).
---

# Creating a Taack Application (`app/<name>`)

## 1. Initial Scaffolding
Generate the plugin skeleton with the dedicated Gradle task:
```bash
./gradlew generateTaackAppTask -DmodName=<appName>
```
This task:
- Creates `app/<appName>/` with `build.gradle` (already depends on `:crew`), `<App>GrailsPlugin.groovy`, `<App>Controller.groovy`, `<App>UiService.groovy`, `messages.properties`, and the SVG icon.
- Automatically adds `implementation project(":<appName>")` in `server/build.gradle` (`settings.gradle` automatically includes all subdirectories of `app/`).
- If the application uses the AsciiDoc editor (`taack.wysiwyg.*`), add to `app/<appName>/build.gradle`:
  ```groovy
  compileOnly "org.taack:taack-ui-asciidoc"
  ```

## 2. Domain Classes (`grails-app/domain/<appName>/`)
- Always annotate with `@GrailsCompileStatic` and `@TaackFieldEnum` (`import taack.ast.annotation.TaackFieldEnum`).
- `@TaackFieldEnum` generates compile-time `<field>_` and `self_` field descriptors required by the UI DSLs (`field obj.name_`, `filterField obj.name_`, `sortableFieldHeader obj.name_`, `rowField obj.name_`).
- Always implement `String toString()` (used for Many-To-One `ajaxField` selection).

## 3. Security (`<App>SecurityService.groovy`)
- **Golden Rule**: Never pollute UI or controller code with manual `if (hasAccess)` checks.
- Create an `<App>SecurityService` annotated with `@GrailsCompileStatic` and `static lazyInit = false`.
- Register access rules in `@PostConstruct void init()` via `TaackUiEnablerService.securityClosure`:
  ```groovy
  private boolean securityCanEdit(Long id, Map p) {
      // check via SpringSecurityService
  }

  @PostConstruct
  void init() {
      TaackUiEnablerService.securityClosure(
          this.&securityCanEdit,
          MyController.&edit as MC,
          MyController.&save as MC,
          MyController.&delete as MC
      )
      TaackAppRegisterService.register(new TaackApp(MyController.&index as MC,
          new String(this.class.getResourceAsStream('/<app>/<app>.svg').readAllBytes())))
  }
  ```
- `ActionDefaultLogInterceptor` and the UI DSLs (`menu`, `subMenu`, `menuIcon`, `rowAction`) automatically invoke `TaackUiEnablerService` and hide unauthorized actions/menus.
- For an action accessible without authentication: annotate the controller action with `@Secured(['permitAll'])` and register a dedicated `securityClosure` if access depends on entity state (e.g., `page.published`).

## 4. Menus (`UiMenuSpecifier` & `MenuSpec`)
- The Taack menu hierarchy supports at most **2 levels**: `menu` (level 1) or `label` containing `subMenu` entries (level 2).
- To build a dynamic menu section (e.g., from the database) via a closure:
  ```groovy
  Closure<MenuSpec> buildDynamicMenuClosure() {
      MenuSpec.buildMenuSpec {
          menu('Title', MyController.&action as MC, [id: 1L])
          label('Group') {
              subMenu('Sub-menu', MyController.&action as MC, [id: 2L])
          }
      }
  }

  UiMenuSpecifier buildMenu(String q = null) {
      new UiMenuSpecifier().ui {
          inline(this.buildDynamicMenuClosure())
          menu MyController.&list as MC
          menuOptions(SupportedLanguage.fromContext())
      }
  }
  ```

## 5. Forms, Tables, and AsciiDoc (`<App>UiService.groovy`)
- **Form (`UiFormSpecifier`)**:
  - Always initialize the domain instance if null: `obj ?= new MyDomain()`.
  - Many-To-One selection (M2O): `ajaxField obj.parent_, MyController.&selectParentM2O as MC` pointing to an action that opens a `modal { tableFilter(filter, tableWithSelectMode) }`.
  - In `UiTableSpecifier` when in selection mode (`selectMode == true`):
    ```groovy
    rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, item.id, item.toString()
    ```
- **AsciiDoc Editor**:
  - In `UiFormSpecifier`:
    ```groovy
    EditorOption.EditorOptionBuilder editor = EditorOption.getBuilder()
    editor.addSpanRegexes(TaackBaseAsciidocSpans.spans)
    editor.addSpanRegexes(TaackAsciidocTable.spans)
    editor.addSpanRegexes(TaackAsciidocPlantUML.spans)
    editor.onDropAction(MyController.&dropEditor as MC, obj.id ? [objId: obj.id] : [:] as Map<String, ? extends Serializable>)
    fieldEditor obj.body_, editor.build()
    ```
  - HTML rendering in `UiShowSpecifier`:
    ```groovy
    String urlRoot = new Parameter().urlMapped(MyController.&downloadBinBodyFiles as MC, [id: obj.id])
    inlineHtml(Asciidoc.getContentHtml(obj.body, urlRoot, false), 'asciidocMain')
    ```

## 6. Images & File Handling (`TaackAttachmentService`)
- **Custom Image/File Domain Class** (e.g., `CmsImage`):
  - `TaackAttachmentService` registers a `TaackSaveService.registerFieldCustomSavingClosure('filePath', ...)` hook. Any domain class declaring `String filePath` (with `filePath widget: 'filePath'` in `constraints`) automatically has its uploaded file stored as `<sha1>.<ext>` and its optional fields (`originalName`, `contentType`, `contentTypeEnum`, `contentTypeCategoryEnum`, `md5sum`, `contentShaOne`, `fileSize`, `width`, `height`) populated when calling `taackSaveService.save(MyImage)`.
  - Register a dedicated storage folder per controller in `@PostConstruct void init()` of the UI service:
    ```groovy
    TaackAttachmentService.filePaths.put('myController', new File(storePath))
    ```
- **WebP Previews (`attachmentPreview`)**:
  - Generate and cache resized WebP previews (`PreviewFormat.DEFAULT`, `PreviewFormat.PREVIEW_MEDIUM`, `PreviewFormat.PREVIEW_LARGE`, etc.) from any stored image file:
    ```groovy
    File preview = taackAttachmentService.attachmentPreview(originalFile, targetPreviewFile, format)
    ```
  - Use `PreviewFormat.DEFAULT` for 64px table thumbnails and `PreviewFormat.PREVIEW_LARGE` for full previews and AsciiDoc body images (`downloadBinBodyFiles`).
- **AsciiDoc Editor Drop & ODF Import**:
  - In `dropEditor`: handle `params.get('onpaste')` via `taackEditorService.convertFromHtml(...)`, ODT/ODS files via `taackEditorService.convert(ISaveImage, inputStream)`, and dropped images via `MyImage img = taackSaveService.save(MyImage)` returning `render "image::${img.originalName}[]"`.

## 7. Controller & Saving (`<App>Controller.groovy`)
- Annotate with `@GrailsCompileStatic` and `@Secured(['ROLE_USER', ...])`.
- Save an entity in a `@Transactional` action:
  ```groovy
  @Transactional
  def save() {
      taackSaveService.saveThenRedirectOrRenderErrors(MyDomain, this.&index as MC)
  }
  ```

## 8. Internationalization (`grails-app/i18n/messages<App>*.properties`)
- **Plugin bundle naming (Critical)**: Because `server` uses `grails { exploded = true }` and has its own `server/grails-app/i18n/messages_*.properties`, any plugin file named `messages_<lang>.properties` will be shadowed on the classpath by `server`'s bundle. Always name plugin i18n bundles with a module-specific prefix such as `messages<App>.properties`, `messages<App>_en.properties`, `messages<App>_fr.properties`, `messages<App>_de.properties`, `messages<App>_zh.properties`, `messages<App>_zh_CN.properties` (like `crew` uses `messagesDomain*.properties` and `messagesAction*.properties`).
- **Key resolution conventions** (`Parameter.trField` & `TaackUiService.tr`):
  - **Domain fields**: `<uncapitalizedDomain>.<fieldName>.label` (e.g., `cmsPage.title.label`), falling back to `default.<fieldName>.label`. Nested fields check `<domain>.<assoc>.<field>.label`, `default.<assoc>.<field>.label`, `<assocDomain>.<field>.label`, `default.<field>.label`. Provide both `<domain>.<field>.label` and `default.<field>.label` so every fallback candidate is covered.
  - **Controller actions / menus / form buttons**: `<controller>.<action>.label` (e.g., `cms.pages.label`, `cms.saveCmsPage.label`), falling back to `default.<action>.label` or `default.<verb>.label` (`create`, `edit`, `delete`, `save`, etc.).
- **Inspecting i18n keys (`lang=test`)**:
  - Append `?lang=test` (or `&lang=test`) to any URL to activate Taack's i18n inspection mode (`Parameter.testI18n` / `LocaleContextHolder.locale.language == 'test'`).
  - Instead of translated text, the UI displays all candidate i18n keys for every label, field, column header, and action button — with keys that already have a translation highlighted in bold (`<b>key</b>`) and missing keys shown in plain text.

## 9. Quick Verification
Always validate Groovy static compilation for both the plugin and its integration in `server`:
```bash
./gradlew :<appName>:compileGroovy :server:compileGroovy
```
