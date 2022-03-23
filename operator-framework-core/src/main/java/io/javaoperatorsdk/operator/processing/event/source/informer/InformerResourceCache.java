package io.javaoperatorsdk.operator.processing.event.source.informer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.javaoperatorsdk.operator.processing.event.ObjectKey;
import io.javaoperatorsdk.operator.processing.event.source.ResourceCache;
import io.javaoperatorsdk.operator.processing.event.source.UpdatableCache;

class InformerResourceCache<T extends HasMetadata> implements ResourceCache<T>, UpdatableCache<T> {

  private final Cache<T> cache;

  public InformerResourceCache(SharedInformer<T> informer) {
    this.cache = (Cache<T>) informer.getStore();
  }

  @Override
  public Optional<T> get(ObjectKey objectKey) {
    return Optional.ofNullable(cache.getByKey(getKey(objectKey)));
  }

  private String getKey(ObjectKey objectKey) {
    return Cache.namespaceKeyFunc(objectKey.getNamespace().orElse(null), objectKey.getName());
  }

  @Override
  public Stream<T> list(Predicate<T> predicate) {
    return cache.list().stream().filter(predicate);
  }

  @Override
  public Stream<T> list(String namespace, Predicate<T> predicate) {
    final var stream = cache.list().stream()
        .filter(r -> namespace.equals(r.getMetadata().getNamespace()));
    return predicate != null ? stream.filter(predicate) : stream;
  }

  @Override
  public Stream<ObjectKey> keys() {
    return cache.listKeys().stream().map(Mappers::fromString);
  }

  @Override
  public T remove(ObjectKey key) {
    return cache.remove(cache.getByKey(getKey(key)));
  }

  @Override
  public void put(ObjectKey key, T resource) {
    // check that key matches the resource
    final var fromResource = ObjectKey.fromResource(resource);
    if (!Objects.equals(key, fromResource)) {
      throw new IllegalArgumentException(
          "Key and resource don't match. Key: " + key + ", resource: " + fromResource);
    }
    cache.put(resource);
  }
}
