#Java Client Tutorial
##Overview
This is a tutorial application for the [Java client library](https://github.com/Genability/genability-java) for Genability's API. The associated tutorial is [here](#), and there are lots of comments in the code that detail how to make and process requests to the API using this library.

This tutorial shows a simple workflow for doing a residential savings analysis based on a person's ZIP code. It utilizes the following Genability API endpoints:

1. [Load Serving Entity](http://developer.genability.com/documentation/api-reference/tariff-api/load-serving-entity/) - `/rest/public/lses`
2. [Account](http://developer.genability.com/documentation/api-reference/account-api/account/) - `/rest/v1/accounts`
3. [Typical Baseline](http://developer.genability.com/documentation/api-reference/tariff-api/typical-baseline/) - `/rest/v1/baselines/best`
4. [Savings Analysis](http://developer.genability.com/documentation/api-reference/switch-api/savings-analysis/) - `/rest/v1/accounts/analysis`

##Usage
1. Create a Genability App from [Genability Explorer](https://apps.genability.com/profile/organizations/current)
2. Export your app ID and app key to `APP_ID` and `APP_KEY` environment variables, respectively.
3. Download the [Java client library](/Genability/genability-java) and install it using `mvn:install`.
4. Download the tutorial app and install it using `mvn:install`.
5. Run the app using `mvn exec:java`.

In summary:

```
export APP_ID=YOUR_APP_ID
export APP_KEY=YOUR_APP_KEY

git clone https://github.com/Genability/genability-java.git
cd genability-java
mvn install
cd ..

git clone https://github.com/Genability/java-client-tutorial.git
cd java-client-tutorial
mvn install
mvn exec:java
```
