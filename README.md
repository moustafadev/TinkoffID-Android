# T-ID

[![Maven Central](https://img.shields.io/maven-central/v/ru.tinkoff.core.tinkoffauth/tid?style=for-the-badge&labelColor=%23FFDD2D&color=%23333333)](https://central.sonatype.com/artifact/ru.tinkoff.core.tinkoffauth/tid)

T-ID - SDK для авторизации пользователей Android приложений с помощью аккаунта Т-Банка.

## Предварительные этапы

Для начала работы с T-ID в качестве партнера заполните заявку на
подключение на [данной странице](https://www.tbank.ru/business/open-api/).
После рассмотрения вашей заявки вы получите по электронной почте
`Client ID` и пароль. Подробная инструкция доступна в [документации](https://developer.tbank.ru/docs/api#section/Partnerskij-scenarij)

## Установка

Для начала работы добавьте следующую строчку в `build.gradle`:

``` groovy
implementation "ru.tinkoff.core.tinkoffauth:tid:${version}"
```

Так же необходимо добавить в [network-security-config](https://developer.android.com/training/articles/security-config) сертификаты от Минцифры и доп. сертификат от Т-Банка.
Пример можно посмотреть в [app-demo](app-demo). Выглядит он так:

``` xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="@raw/tinkoff_root_cert_production" />
            <certificates src="@raw/ministry_of_digital_development_root_cert" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

## Требования к приложению

Для работы SDK необходимо следующее:

+ Зарегистрированный идентификатор авторизуемого приложения (`client_id`)
+ Uri авторизуемого клиента, на который будет выполнен редирект по завершению авторизации (`redirectUri`)
+ Зарегистрированный авторизуемым приложением [AppLink](https://developer.android.com/training/app-links), который будет использоваться для возврата в приложение после авторизации. Использование
  DeepLink возможно, но не рекомендовано. (`callbackUrl`)
+ Авторизация через приложение Т-Банка будет работать только начиная с версии Android 6.0, на более ранних версиях получить авторизационные данные не будет возможности.

## Интеграция

### Начало

Все необходимое взаимодействие в библиотеке идет только через класс `TidAuth`.

Чтобы начать авторизацию через приложение Т-Банка, создайте Intent с помощью `tidAuth.createTidAuthIntent(callbackUrl, webMode)` и запустите его.
Внутри этого метода автоматически происходит создание Intent для открытия приложения Т-Банка, если оно доступно, иначе создается Intent
для открытия `TidWebViewAuthActivity`, в котором возможно прохождение авторизации через веб Т-Банка.

Вы также сами можете выбирать, какой способ авторизации использовать.  
Сперва проверьте, есть ли возможность пройти авторизацию через приложение Т-Банка.
Для этого используйте `tidAuth.isTBankAppAuthAvailable()`.
Если флаг `isTBankAppAuthAvailable == true`, значит у пользователя установлено приложение Т-Банка,
через которое можно осуществить вход, используя `tidAuth.createTBankAppAuthIntent(callbackUrl)`.
Иначе можно запустить сценарий авторизации через WebView, используя `tidAuth.createTidWebViewAuthIntent(callbackUrl, webMode)`.

### Выполнение авторизации

Для авторизации:

1. Необходимо создать объект `TidAuth(applicationContext, clientId, redirectUri, tidApiHost)` - это основной класс для работы с библиотекой.
2. Создать `Intent` для выбранного способа авторизации, передав в качестве аргумента:
    - `callbackUrl` - ваш AppLink/DeepLink (по данному uri приложение Т-Банка вернется обратно после процесса авторизации), запустить его
    - `webMode` - тип Web авторизации:
        - `Parther` (по умолчанию) - Web авторизация под брендом T-ID
        - [`WhiteLabel(val phone: String)`](https://developer.tbank.ru/docs/products/scenarios/TID/widget-white-label) - обезличенный дизайн формы ввода ОТП. Поддерживаются только российские номера в формате (`+79999999999`), иначе бросается исключение `TidInvalidPhoneException` при формировании `Intent`

``` kotlin
val intent = tidAuth.createTidAuthIntent(
    callbackUrl = "https://example.com/callback",
    webMode = TidWebMode.Partner, // TidWebMode.WhiteLabel(phone = "+79999999999")
)
startActivity(intent)
```

3. После прохождения пользователем авторизации через Т-Банк, произойдет переход в ваше приложение на основе `callbackUrl`. В `intent.data` будет храниться информация по авторизации.

### Завершение авторизации

Успешность авторизации можно проверить методом - `tidAuth.getStatusCode(uriFromIntentData)`. Метод вернет статус `SUCCESS` или `CANCELLED_BY_USER`

При успешной авторизации:

1. Нужно получить `TidTokenPayload`, выполнив `getTidTokenPayload(uri).getResponse()`. Данный метод необходимо выполнять в отличном от main потоке.
2. Полученный `TidTokenPayload` будет содержать учетные данные пользователя

При неуспешной авторизации придет статус в `intent.data` `CANCELLED_BY_USER` или приложение откроется без данных в `intent.data`.

### Перевыпуск авторизационных данных

Для перевыпуска accessToken необходимо использовать метод

``` kotlin
fun obtainTokenPayload(refreshToken: String): TidCall<TidTokenPayload>
```

В него нужно передать refreshToken, полученный ранее. Выполнять вызов `getResponse()` необходимо не на main потоке

### Отзыв авторизационных данных

Иногда может возникнуть ситуация, когда полученные авторизационные данные более не нужны.
Например, при выходе, смене или отключении аккаунта пользователя в авторизованном приложении.
В таком случае, приложению необходимо выполнить отзыв авторизационных данных с помощью методов:

``` kotlin
fun signOutByAccessToken(accessToken: String): TidCall<Unit>
fun signOutByRefreshToken(refreshToken: String): TidCall<Unit>
```

### Хранение Refresh Token

Желательно реализовать безопасное хранение `TidTokenPayload.refreshToken` в приложении,
так как он необходим для перевыпуска токенов.

### UI

Чтобы подробнее ознакомиться со стайлгайдом по размещению кнопок, перейдите по [ссылке](https://acdn.t-bank-app.ru/static/documents/tinkoff-id-button-placement-guidelines.pdf)

Библиотека предоставляет кнопку `TidSignInButton.kt`.
Пример по ее настройке доступен в `PartnerActivity.kt` и `activity_partner.xml`.
Форма кнопки настраивается через атрибут `app:tid_compact`, который может иметь значение `true` или `false` (по умолчанию).
Стиль кнопки можно изменить с помощью атрибута `app:tid_style`, который может иметь значение `white` / `gray` / `black` / `primary` (по умолчанию).
Если кнопка имеет стандартную форму, то можно дополнительно настроить основной текст (`app:tid_title`), радиусы углов (`app:tid_corner_radius`) и шрифт текста (`app:tid_font`).
Пример добавления кнопки:

``` xml
<ru.tbank.core.tid.ui.TidSignInButton 
    android:id="@+id/standardButtonTidAuth"
    android:layout_width="wrap_content"
    android:layout_height="60dp"
    app:tid_compact="false"
    app:tid_title="Sign in with"
    app:tid_style="primary"
    app:tid_corner_radius="8dp"
    app:tid_font="@font/neue_haas_unica_w1g" />
```

#### Виды кнопок:

|                             |                     style = `primary`                      |
|-----------------------------|:----------------------------------------------------------:|
| Default                     |   ![default](imgs/tid_sign_in_button/default_button.png)   |
| With text (`app:tid_title`) | ![with_text](imgs/tid_sign_in_button/with_text_button.png) |
| Compact (`app:tid_compact`) |   ![compact](imgs/tid_sign_in_button/compact_button.png)   |

## Структура публичной части SDK

### TidAuth

Базовый класс для работы c авторизацией

| Функция                                                                     | Описание                                                                                                                                                                                                                                                                                                   |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `createTidAuthIntent(callbackUrl: Uri, webMode: TidWebMode): Intent`        | Создает Intent для открытия приложения Т-Банка, если оно доступно, иначе для `TidWebViewAuthActivity`, чтобы авторизоваться через веб Т-Банка. На вход принимает Uri для создания AppLink/DeepLink, по которому будет осуществлен переход после окончания процесса авторизации и режим для Web авторизации |
| `isTBankAppAuthAvailable(): Boolean`                                        | Выполняет проверку возможна ли авторизация через приложения Т-Банка на данном устройстве                                                                                                                                                                                                                   |
| `createTBankAppAuthIntent(callbackUrl: Uri): Intent`                        | Создает Intent для открытия приложения Т-Банка для прохождения авторизации. На вход принимает Uri для создания AppLink/DeepLink, по которому будет осуществлен переход после окончания процесса авторизации                                                                                                |
| `createTidWebViewAuthIntent(callbackUrl: Uri, webMode: TidWebMode): Intent` | Создает Intent для открытия `TidWebViewAuthActivity` для прохождения авторизации в вебе. На вход принимает Uri для создания AppLink/DeepLink, по которому будет осуществлен переход после окончания процесса авторизации и режим для Web авторизации                                                       |
| `getTidTokenPayload(uri: Uri): TidCall<TidTokenPayload>`                    | Возвращает объект, который позволяет получить синхронно информацию о токене, которая придет к вам в `intent.data` после авторизации через Т-Банк                                                                                                                                                           |
| `getStatusCode(uri: Uri): TidStatusCode?`	                                  | Позволяет получить статус выполнения авторизации из `intent.data` пришедшего к вам                                                                                                                                                                                                                         |
| `obtainTokenPayload(refreshToken: String): TidCall<TidTokenPayload>`        | Возвращает объект, который позволяет синхронно обновить токен по рефреш токену полученному ранее                                                                                                                                                                                                           |
| `signOutByAccessToken(accessToken: String): TidCall<Unit>`                  | Возвращает объект, который позволяет синхронно разлогинить по accessToken                                                                                                                                                                                                                                  |
| `signOutByRefreshToken(refreshToken: String): TidCall<Unit>`                | Возвращает объект, который позволяет синхронно разлогинить по refreshToken                                                                                                                                                                                                                                 |

### TidCall`<T>`

Объект, который позволяет выполнить долгий запрос синхронно и получить `T`  или отменить этот запрос.

| Функция                | Описание                           |
|------------------------|------------------------------------|
| `fun getResponse(): T` | Позволяет получить объект типа `T` |
| `cancel()`             | Отменяет запрос                    |

### TidTokenPayload

В результате успешной авторизации приложение получает объект `TidTokenPayload`, содержащий следующие свойства:

+ `accessToken` - токен для обращения к API Т-Банка
+ `refreshToken` - токен, необходимый для получения нового `accessToken`
+ `idToken` - идентификатор пользователя в формате JWT
+ `expiresIn` - время, через которое `accessToken` станет неактуальным и нужно будет получить новый с помощью `obtainTokenPayload(refreshToken)`

### TidStatusCode

Enum со статусами пришедшими из приложения Т-Банка после попытки авторизации

| Значение            | Описание                                          |
|---------------------|---------------------------------------------------|
| `SUCCESS`           | Авторизация в Т-Банке прошла успешно              |
| `CANCELLED_BY_USER` | Авторизация в Т-Банке была отменена пользователем |

### TidRequestException

Ошибка, происходящая во время выполнения запросов

| Параметр                         | Описание                                                             |
|----------------------------------|----------------------------------------------------------------------|
| `reason: Throwable`              | Причина ошибки                                                       |
| `message: String?`               | Сообщение к ошибке                                                   |
| `errorMessage: TidErrorMessage?` | Объект описывающий ошибку и причину происхождения с точки зрения апи |

### TidErrorMessage

Объект, описывающий ошибку и причину происхождения с точки зрения апи

| Параметр                    | Описание                                                                               |
|-----------------------------|----------------------------------------------------------------------------------------|
| `message: String?`          | Человекочитаемый тип ошибки от АПИ                                                     |
| `public val errorType: Int` | Тип ошибки, принимающий значение из TidTokenErrorConstants, TokenSignOutErrorConstants |

### TidTokenErrorConstants

Ошибки, возвращаемые во время получения и обновления токена

| Значение                 | Описание                                                                                                                   |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `INVALID_REQUEST`        | Не хватает обязательных параметров, кук, заголовков и т.д.                                                                 |
| `INVALID_CLIENT`         | redirect_uri не соответствует клиенту                                                                                      |
| `INVALID_GRANT`          | Передан невалидный (протухший) refresh_token или code                                                                      |
| `UNAUTHORIZED_CLIENT`    | Нет заголовка                                                                                                              |
| `UNSUPPORTED_GRANT_TYPE` | Передан неизвестный grant_type                                                                                             |
| `SERVER_ERROR`           | Что-то пошло не так, возможно стоит перезапустить авторизационный диалог                                                   |
| `LIMIT_EXCEEDED`         | Приложению слишком часто запрашивает выпуск токенов (текущий лимит 50 в час), возможно, стоит поискать ошибку в приложении |
| `UNKNOWN_ERROR`          | Вернулся неизвестный тип ошибки                                                                                            |


### TidInvalidPhoneException

Ошибка, происходящая при попытке запустить авторизацию в WebView в режиме `TidWebMode.WhiteLabel` с некорректным номером телефона. Поддерживаются только российские номера в формате `+79999999999`

### TidWebMode

`sealed interface` с типом Web авторизации

| Наследник                                        | Описание                                                                                                                                                                                                  |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `object TidWebMode.Parther`                      | Значение по умолчанию. Авторизация в WebView с брендом T-ID                                                                                                                                               |
| `class TidWebMode.WhiteLabel(val phone: String)` | Авторизация в WebView с обезличенным дизайном формы ввода ОТП.<br/>Поле `phone` поддерживает только российские номера в формате `+79999999999`, иначе будет брошено исключение `TidInvalidPhoneException` |

## Example

SDK поставляется с примером приложения, где можно посмотреть работу авторизации.
В папке [app-demo](app-demo) содержится тестовое приложение.
В данном приложении можно:

- Проверить работу вашего `clientId`, подставив его в необходимое поле
- Посмотреть варианты дизайна кнопки
- Авторизоваться через приложение Т-Банка или WebView (в режиме `Parther` или `WhiteLabel`)
- Отозвать токены
- Обновить токены

Для авторизации так же требуется установить хотя бы одно из приложений Т-Банка (сейчас поддерживает авторизацию только приложение Мобильного Банка).

## Поддержка

- По возникающим вопросам просьба обращаться на tid_support@tbank.ru
- Баги и feature-реквесты можно направлять в раздел [issues](https://opensource.tbank.ru/mobile-tech/T-ID-Android/-/issues)
