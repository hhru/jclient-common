## Как делать external* вызовы через jclient?

При вызове нужно отметить флаг **ru.hh.jclient.common.HttpClient#external**; [Пример](https://forgejo.pyn.ru/hhru/hh.ru/src/tag/25.40.4/billing-webapp/src/main/java/ru/hh/payment/system/raiffeisen/client/RaiffeisenClient.java#L75)

[настройка upstream-config.yml](https://wiki.hh.ru/pages/viewpage.action?pageId=322923129)

#### Пояснения
**external вызов** - это вызов во внешнюю систему относительно HH. Осуществляется через обычный url вида http(s):\\serviceUrl:port. При external вызове не будут переданы внутренние заголовки HH