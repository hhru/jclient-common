# How balancing works
## External requests
external request = request with no upstream described for host  
served with [default configuration](./src/main/java/ru/hh/jclient/common/balancing/RequestBalancerBuilder.java#L34-L37)
## Internal requests
### Balancing
//TODO
### Arch
Upstream - configuration(retries amount and policy, timeout etc) + ip and its weight list to balance request with 
If our default Consul-based implementation described below is not applicable for you - there are 2 extension points:
- Implement custom [UpstreamService](./src/main/java/ru/hh/jclient/consul/UpstreamService.java) and [UpstreamConfigService](./src/main/java/ru/hh/jclient/consul/UpstreamConfigService.java) to use it with our [BalancingUpstreamManager](./src/main/java/ru/hh/jclient/common/balancing/BalancingUpstreamManager.java) - it requires to implement pub-sub pattern with your storage
- If [BalancingUpstreamManager](./src/main/java/ru/hh/jclient/common/balancing/BalancingUpstreamManager.java) looks too constrained - feel free to implement [UpstreamManager](./src/main/java/ru/hh/jclient/common/balancing/UpstreamManager.java) - this abstraction is very high)
#### Data Source
> Important. Service name for configuration in consul KV must be equal to service name in consul registration of the service 

1. Configuration format: [ApplicationConfig](./src/main/java/ru/hh/jclient/consul/model/ApplicationConfig.java) must be set in Consul KV
2. Client subscribes for Consul KV entries for requried services
3. Client subsribes on Consul service health to get IP and weight list for required services
4. We register instances in consul with related weight in any way Consul supports. We recommend do it with [nuts-and-bolts](https://github.com/hhru/nuts-and-bolts) - the frameworks will register instance on startup
5. profit
##### For hh.ru
- Раскатка конфигов в Consul: из [cassandra_settings/balancer.yml](https://github.com/hhru/deploy/blob/master/public/playbooks/roles/cassandra_settings/vars/balancer.yml). См. [EXP-41677](https://jira.hh.ru/browse/EXP-41677)
- Задача на приведение имени сервиса и апстрима к одному значению: [EXP-44785](https://jira.hh.ru/browse/EXP-44785)
- Сервисы, которые сами не умеют регистрироваться регаются скриптом: [EXP-37504](https://jira.hh.ru/browse/EXP-37504)
#### Previous LEGACY structure (2.x.x)
1. Configuration must be provided in any way you want but in specific format. See 2.x.x sources to get right structure of the config
2. Configuration update is also up to you) You just call updateUpstreams when it's required
3. profit
##### For hh.ru
- Для доставки апстримов мы используем динамические настройки: [settings-client](https://github.com/hhru/settings-client)  
- Раскатка конфигов в cassandra: из [cassandra_settings/balancer.yml](https://github.com/hhru/deploy/blob/master/public/playbooks/roles/cassandra_settings/vars/balancer.yml) через [cassandra_settings.yml](https://github.com/hhru/deploy-dev/blob/master/public/playbooks/cassandra_settings.yml) конфиги И список ip И их весов для апстримов  
- Посмотреть апстримы `select * from settings.setting where service='system'`  
