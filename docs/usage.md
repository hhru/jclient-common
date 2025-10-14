## Как делать external вызовы через jclient?

Jclient считает, что вызов external, если не находит нужный upstream в консуле. 
Это возможно в двух случаях
1. Если указан внешний url вида http(s)://serviceUrl:portб
2. Если нужный upstream не зарегистрирован к консуле. Этот пункт актуален только для тестовой среды. 
На продакшене установлен флаг jclient.ignoreNoServersInCurrentDC=false. При запуске сервис упадет с ошибкой если не найдет нужный upstream.  

Для корректной передачи внутренних заголовков при вызове нужно отметить флаг **ru.hh.jclient.common.HttpClient#external**; [Пример](https://forgejo.pyn.ru/hhru/hh.ru/src/tag/25.40.4/billing-webapp/src/main/java/ru/hh/payment/system/raiffeisen/client/RaiffeisenClient.java#L75)

## Настройка ретраев и таймаутов для upstream
[wiki](https://wiki.hh.ru/pages/viewpage.action?pageId=322923129)
