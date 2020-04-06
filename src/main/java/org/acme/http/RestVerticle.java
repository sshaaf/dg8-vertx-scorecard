package org.acme.http;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import org.acme.cache.CacheVerticle;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RestVerticle extends CacheVerticle {

    final String EP = "/api/score";

    private final Logger logger = Logger.getLogger(RestVerticle.class.getName());

    @Override
    protected void init(Future<Void> startFuture) {
        String host = config().getString("http.host", "localhost");
        int port = config().getInteger("http.port", 8888);


        Router router = Router.router(vertx);
        router.get("/").handler(rc -> {
            rc.response().putHeader("content-type", "text/html")
                    .end(" REST Service");
        });

        router.get(EP).handler(this::getAll);

        router.get("/m/console").handler(StaticHandler.create());
        router.route().handler(BodyHandler.create());

        //router.get(EP).handler(this::getAll);


        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx)
                .register("health", f -> f.complete(Status.OK()));
        router.get("/health").handler(healthCheckHandler);

        router.route("/metrics").handler(PrometheusScrapingHandler.create());


        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, ar -> {
                    if (ar.succeeded()) {
                        startFuture.complete();
                        logger.info("Http Server Listening on: "+port);
                    } else {
                        startFuture.fail(ar.cause());
                    }
                });
    }


    private void getAll(RoutingContext routingContext) {

        Set<String> entries = defaultCache.keySet();

        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(entries.stream().collect(Collectors.toList())));
    }

}
