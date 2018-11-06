/*
National Crime Agency (c) Crown Copyright 2018

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.gov.nca.graph.mapper.datasources;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source for reading from Elasticsearch
 */
public class ElasticDataSource implements DataSource {

  private RestHighLevelClient client;
  private Iterator<SearchHit> searchHits;
  private String scrollId;

  private boolean includeIndex;

  private static final TimeValue SCROLL_TIME_VALUE = TimeValue.timeValueMinutes(5);
  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticDataSource.class);

  public ElasticDataSource(HttpHost host, String username, String password, SearchRequest searchRequest){
    this(host, username, password, searchRequest, false);
  }

  public ElasticDataSource(HttpHost host, String username, String password, SearchRequest searchRequest, boolean includeIndex){
    if(searchRequest.scroll() == null)
      searchRequest.scroll(SCROLL_TIME_VALUE);

    RestClientBuilder builder = RestClient.builder(host);
    if(username != null && !username.isEmpty() && password != null && !password.isEmpty()){
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

      builder = builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
          .setDefaultCredentialsProvider(credentialsProvider)
      );
    }

    client = new RestHighLevelClient(builder);

    SearchResponse searchResponse;
    try {
      searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      LOGGER.error("Unable to retrieve search results", e);
      return;
    }

    searchHits = searchResponse.getHits().iterator();
    scrollId = searchResponse.getScrollId();

    this.includeIndex = includeIndex;
  }

  @Override
  public boolean hasNext() {
    if(searchHits == null)
      return false;

    if(searchHits.hasNext()) {
      return true;
    }else{
      SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(SCROLL_TIME_VALUE);

      SearchResponse searchScrollResponse;
      try {
        searchScrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        LOGGER.error("Unable to retrieve search results", e);
        return false;
      }
      scrollId = searchScrollResponse.getScrollId();

      searchHits = searchScrollResponse.getHits().iterator();
      return searchHits.hasNext();
    }
  }

  @Override
  public Map<String, Object> next() {
    SearchHit hit = searchHits.next();
    Map<String, Object> map = hit.getSourceAsMap();

    if(includeIndex){
      map.put("_index", hit.getIndex());
    }

    return map;
  }

  @Override
  public void close() {
    ClearScrollRequest request = new ClearScrollRequest();
    request.addScrollId(scrollId);

    try {
      client.clearScroll(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      LOGGER.warn("Error clearing scroll context", e);
    }
  }
}
