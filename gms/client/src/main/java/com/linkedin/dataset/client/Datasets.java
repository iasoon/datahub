package com.linkedin.dataset.client;

import com.linkedin.common.client.DatasetsClient;
import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.data.template.StringArray;
import com.linkedin.dataset.*;
import com.linkedin.metadata.query.AutoCompleteResult;
import com.linkedin.metadata.query.BrowseResult;
import com.linkedin.metadata.snapshot.DatasetSnapshot;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.Client;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.linkedin.metadata.dao.utils.QueryUtils.newFilter;

public class Datasets extends DatasetsClient {
    private static final DatasetsRequestBuilders DATASETS_REQUEST_BUILDERS = new DatasetsRequestBuilders();

    public Datasets(@Nonnull Client restliClient) {
        super(restliClient);
    }

    /**
     * Gets {@link Dataset} model for the given urn
     *
     * @param urn dataset urn
     * @return {@link Dataset} dataset model
     * @throws RemoteInvocationException
     */
    @Nonnull
    public Dataset get(@Nonnull DatasetUrn urn)
            throws RemoteInvocationException {
        GetRequest<Dataset> getRequest = DATASETS_REQUEST_BUILDERS.get()
                .id(new ComplexResourceKey<>(toDatasetKey(urn), new EmptyRecord()))
                .build();

        return _client.sendRequest(getRequest).getResponse().getEntity();
    }

    /**
     * Searches for datasets matching to a given query and filters
     *
     * @param input search query
     * @param requestFilters search filters
     * @param start start offset for search results
     * @param count max number of search results requested
     * @return Snapshot key
     * @throws RemoteInvocationException
     */
    @Nonnull
    public CollectionResponse<Dataset> search(@Nonnull String input, @Nonnull Map<String, String> requestFilters,
                                               int start, int count) throws RemoteInvocationException {

        DatasetsFindBySearchRequestBuilder requestBuilder = DATASETS_REQUEST_BUILDERS
                .findBySearch()
                .inputParam(input)
                .filterParam(newFilter(requestFilters)).paginate(start, count);
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Gets browse snapshot of a given path
     *
     * @param query search query
     * @param field field of the dataset
     * @param requestFilters autocomplete filters
     * @param limit max number of autocomplete results
     * @throws RemoteInvocationException
     */
    @Nonnull
    public AutoCompleteResult autoComplete(@Nonnull String query, @Nonnull String field,
                                           @Nonnull Map<String, String> requestFilters,
                                           @Nonnull int limit) throws RemoteInvocationException {
        DatasetsDoAutocompleteRequestBuilder requestBuilder = DATASETS_REQUEST_BUILDERS
                .actionAutocomplete()
                .queryParam(query)
                .fieldParam(field)
                .filterParam(newFilter(requestFilters))
                .limitParam(limit);
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Gets browse snapshot of a given path
     *
     * @param path path being browsed
     * @param requestFilters browse filters
     * @param start start offset of first dataset
     * @param limit max number of datasets
     * @throws RemoteInvocationException
     */
    @Nonnull
    public BrowseResult browse(@Nonnull String path, @Nullable Map<String, String> requestFilters,
                               int start, int limit) throws RemoteInvocationException {
        DatasetsDoBrowseRequestBuilder requestBuilder = DATASETS_REQUEST_BUILDERS
                .actionBrowse()
                .pathParam(path)
                .startParam(start)
                .limitParam(limit);
        if (requestFilters != null) {
            requestBuilder.filterParam(newFilter(requestFilters));
        }
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Gets browse path(s) given dataset urn
     *
     * @param urn urn for the entity
     * @return list of paths given urn
     * @throws RemoteInvocationException
     */
    @Nonnull
    public StringArray getBrowsePaths(@Nonnull DatasetUrn urn) throws RemoteInvocationException {
        DatasetsDoGetBrowsePathsRequestBuilder requestBuilder = DATASETS_REQUEST_BUILDERS
                .actionGetBrowsePaths()
                .urnParam(urn);
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Batch gets list of {@link Dataset} models
     *
     * @param urns list of dataset urn
     * @return map of {@link Dataset} models
     * @throws RemoteInvocationException
     */
    @Nonnull
    public Map<DatasetUrn, Dataset> batchGet(@Nonnull Set<DatasetUrn> urns)
        throws RemoteInvocationException {
        BatchGetEntityRequest<ComplexResourceKey<DatasetKey, EmptyRecord>, Dataset> batchGetRequest
            = DATASETS_REQUEST_BUILDERS.batchGet()
            .ids(urns.stream().map(this::getKeyFromUrn).collect(Collectors.toSet()))
            .build();

        return _client.sendRequest(batchGetRequest).getResponseEntity().getResults()
            .entrySet().stream().collect(Collectors.toMap(
                entry -> getUrnFromKey(entry.getKey()),
                entry -> entry.getValue().getEntity())
            );
    }

    /**
     * Gets latest full dataset snapshot given dataset urn
     *
     * @param datasetUrn dataset urn
     * @return latest full dataset snapshot
     * @throws RemoteInvocationException
     */
    public DatasetSnapshot getLatestFullSnapshot(@Nonnull DatasetUrn datasetUrn) throws RemoteInvocationException {
        DatasetsDoGetSnapshotRequestBuilder requestBuilder = DATASETS_REQUEST_BUILDERS
                .actionGetSnapshot()
                .urnParam(datasetUrn.toString());
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    @Nonnull
    private ComplexResourceKey<DatasetKey, EmptyRecord> getKeyFromUrn(@Nonnull DatasetUrn urn) {
        return new ComplexResourceKey<>(toDatasetKey(urn), new EmptyRecord());
    }

    @Nonnull
    private DatasetUrn getUrnFromKey(@Nonnull ComplexResourceKey<DatasetKey, EmptyRecord> key) {
        return toDatasetUrn(key.getKey());
    }
}