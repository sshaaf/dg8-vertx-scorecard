package org.acme.cache;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;


public abstract class CacheVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(CacheVerticle.class.getName());

    protected RemoteCacheManager client;
    protected RemoteCache<String, String> defaultCache;

    protected abstract void init(Future<Void> startFuture);

    @Override
    public void start(Future<Void> startFuture) {

        vertx.<RemoteCache<String, String>>executeBlocking(fut -> {
            client = new RemoteCacheManager(getConfigBuilder().build());

            // FIX this: potential breaking change
            String cacheName = config().getString("jdg.app.name");
            RemoteCache<String, String> cache = client.getCache(cacheName);
            fut.complete(cache);

        }, res -> {
            if (res.succeeded()) {
                defaultCache = res.result();
                logger.info("Cache connection successfully done to: "+defaultCache.getName());
                init(startFuture);
            } else {
                logger.fatal("Cache connection error");
                startFuture.fail(res.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (client != null) {
            client.stopAsync().whenComplete((e, ex) -> stopFuture.complete());
        } else
            stopFuture.complete();
    }


    protected ConfigurationBuilder getConfigBuilder() {
        ConfigurationBuilder cfg = null;
        if (System.getenv("KUBERNETES_NAMESPACE") != null) {

            cfg = ClientConfiguration.create(config().getString("jdg.svc.name"),
                    config().getString("jdg.app.name"),
                    config().getString("jdg.app.user.name"),
                    config().getString("jdg.app.user.password"));
        } else { // Incase running local
            cfg = new ConfigurationBuilder().addServer()
                    .host(config().getString("jdg.host", "localhost"))
                    .port(config().getInteger("jdg.port", 11222))
                    .clientIntelligence(ClientIntelligence.BASIC);
        }

        return cfg;

    }

}