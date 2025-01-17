package io.javaoperatorsdk.operator.api.config.informer;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.config.DefaultResourceConfiguration;
import io.javaoperatorsdk.operator.api.config.ResourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

@SuppressWarnings("rawtypes")
public interface InformerConfiguration<R extends HasMetadata, P extends HasMetadata>
    extends ResourceConfiguration<R> {

  class DefaultInformerConfiguration<R extends HasMetadata, P extends HasMetadata> extends
      DefaultResourceConfiguration<R> implements InformerConfiguration<R, P> {

    private final SecondaryToPrimaryMapper<R> secondaryToPrimaryResourcesIdSet;
    private final PrimaryToSecondaryMapper<P> associatedWith;

    protected DefaultInformerConfiguration(String labelSelector,
        Class<R> resourceClass,
        SecondaryToPrimaryMapper<R> secondaryToPrimaryResourcesIdSet,
        PrimaryToSecondaryMapper<P> associatedWith,
        Set<String> namespaces) {
      super(labelSelector, resourceClass, namespaces);
      this.secondaryToPrimaryResourcesIdSet =
          Objects.requireNonNullElse(secondaryToPrimaryResourcesIdSet,
              Mappers.fromOwnerReference());
      this.associatedWith =
          Objects.requireNonNullElseGet(associatedWith, () -> ResourceID::fromResource);
    }


    public SecondaryToPrimaryMapper<R> getPrimaryResourcesRetriever() {
      return secondaryToPrimaryResourcesIdSet;
    }

    public PrimaryToSecondaryMapper<P> getAssociatedResourceIdentifier() {
      return associatedWith;
    }

  }

  SecondaryToPrimaryMapper<R> getPrimaryResourcesRetriever();

  PrimaryToSecondaryMapper<P> getAssociatedResourceIdentifier();

  @SuppressWarnings("unused")
  class InformerConfigurationBuilder<R extends HasMetadata, P extends HasMetadata> {

    private SecondaryToPrimaryMapper<R> secondaryToPrimaryResourcesIdSet;
    private PrimaryToSecondaryMapper<P> associatedWith;
    private Set<String> namespaces;
    private String labelSelector;
    private final Class<R> resourceClass;

    private InformerConfigurationBuilder(Class<R> resourceClass) {
      this.resourceClass = resourceClass;
    }

    public InformerConfigurationBuilder<R, P> withPrimaryResourcesRetriever(
        SecondaryToPrimaryMapper<R> secondaryToPrimaryMapper) {
      this.secondaryToPrimaryResourcesIdSet = secondaryToPrimaryMapper;
      return this;
    }

    public InformerConfigurationBuilder<R, P> withAssociatedSecondaryResourceIdentifier(
        PrimaryToSecondaryMapper<P> associatedWith) {
      this.associatedWith = associatedWith;
      return this;
    }


    public InformerConfigurationBuilder<R, P> withNamespaces(String... namespaces) {
      this.namespaces = namespaces != null ? Set.of(namespaces) : Collections.emptySet();
      return this;
    }

    public InformerConfigurationBuilder<R, P> withNamespaces(Set<String> namespaces) {
      this.namespaces = namespaces != null ? namespaces : Collections.emptySet();
      return this;
    }


    public InformerConfigurationBuilder<R, P> withLabelSelector(String labelSelector) {
      this.labelSelector = labelSelector;
      return this;
    }

    public InformerConfiguration<R, P> build() {
      return new DefaultInformerConfiguration<>(labelSelector, resourceClass,
          secondaryToPrimaryResourcesIdSet, associatedWith,
          namespaces);
    }
  }

  static <R extends HasMetadata, P extends HasMetadata> InformerConfigurationBuilder<R, P> from(
      EventSourceContext<P> context, Class<R> resourceClass) {
    return new InformerConfigurationBuilder<>(resourceClass);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static InformerConfigurationBuilder from(Class resourceClass) {
    return new InformerConfigurationBuilder<>(resourceClass);
  }

  static <R extends HasMetadata, P extends HasMetadata> InformerConfigurationBuilder<R, P> from(
      InformerConfiguration<R, P> configuration) {
    return new InformerConfigurationBuilder<R, P>(configuration.getResourceClass())
        .withNamespaces(configuration.getNamespaces())
        .withLabelSelector(configuration.getLabelSelector())
        .withAssociatedSecondaryResourceIdentifier(
            configuration.getAssociatedResourceIdentifier())
        .withPrimaryResourcesRetriever(configuration.getPrimaryResourcesRetriever());
  }
}
