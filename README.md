#Java Client Tutorial
##Overview
This is a tutorial application for the [Java client library](/Genability/genability-java) for Genability's API. The associated tutorial is [here](#) It shows a simple workflow for doing a residential savings analysis based on a person's ZIP code. It utilizes the following Genability API endpoints:

1. [Load Serving Entity](http://developer.genability.com/documentation/api-reference/tariff-api/load-serving-entity/) - `/rest/public/lses`
2. [Account](http://developer.genability.com/documentation/api-reference/account-api/account/) - `/rest/v1/accounts`
3. [Typical Baseline](http://developer.genability.com/documentation/api-reference/tariff-api/typical-baseline/) - `/rest/v1/baselines/best
4. [Savings Analysis](http://developer.genability.com/documentation/api-reference/switch-api/savings-analysis/) - `/rest/v1/accounts/analysis`

##Usage
1. Download the [Java client library](/Genability/genability-java) and install it using `mvn:install`.
2. Download the tutorial app and install it using `mvn:install`.
3. Run the app using `mvn exec:java`.

In summary:

```
git clone https://github.com/Genability/genability-java.git
cd genability-java
mvn install
cd ..

git clone https://github.com/Genability/java-client-tutorial.git
cd java-client-tutorial
mvn install
mvn exec:java
```
