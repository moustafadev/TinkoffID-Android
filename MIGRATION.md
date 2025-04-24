# Документ по миграции

## TinkoffID 1.3.0 -> T-ID 1.0.0

Все классы были переименованы в соответствии с новым названием библиотеки:
`TinkoffId` -> `Tid`, `Tinkoff` -> `Tid`
Также, изменилось имя пакета: `ru.tinkoff.core.tinkoffId` -> `ru.tbank.core.tid`

Полная таблица:

| Before                                                 |    | After                                             |
|:-------------------------------------------------------|:--:|:--------------------------------------------------|
| `ru.tinkoff.core.tinkoffId.TinkoffErrorMessage`        | -> | `ru.tbank.core.tid.TidErrorMessage`               |
| `ru.tinkoff.core.tinkoffId.TinkoffLoggingConstants`    | -> | `ru.tbank.core.tid.TidLoggingConstants`           |
| `ru.tinkoff.core.tinkoffId.TinkoffRequestException`    | -> | `ru.tbank.core.tid.TidRequestException`           |
| `ru.tinkoff.core.tinkoffId.TinkoffTokenErrorConstants` | -> | `ru.tbank.core.tid.TidTokenErrorConstants`        |
| `ru.tinkoff.core.tinkoffId.TokenSignOutErrorConstants` | -> | `ru.tbank.core.tid.TidTokenSignOutErrorConstants` |
| `ru.tinkoff.core.tinkoffId.TinkoffIdSignInButton`      | -> | `ru.tbank.core.tid.TidSignInButton`               |
| `ru.tinkoff.core.tinkoffId.TinkoffIdAuth`              | -> | `ru.tbank.core.tid.TidAuth`                       |
| `ru.tinkoff.core.tinkoffId.TinkoffCall`                | -> | `ru.tbank.core.tid.TidCall`                       |
| `ru.tinkoff.core.tinkoffId.TinkoffIdStatusCode`        | -> | `ru.tbank.core.tid.TidStatusCode`                 |
| `ru.tinkoff.core.tinkoffId.TinkoffTokenPayload`        | -> | `ru.tbank.core.tid.TidTokenPayload`               |

Кроме этого, был изменен префикс в названиях ресурсов: `tinkoff_id` -> `tid`

Таблица с примерами:

| Before                            |    | After                    |
|:----------------------------------|:--:|:-------------------------|
| `@drawable/tinkoff_id_close`      | -> | `@drawable/tid_close`    |
| `@color/tinkoff_id_black_text`    | -> | `@color/tid_black_text`  |
| `@string/tinkoff_id_tinkoff_text` | -> | `@string/tid_tbank_text` |
