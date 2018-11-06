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

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Data source for reading from a Java iterator
 */
public class IteratorDataSource<T> implements DataSource {

  private Iterator<T> iter;
  private Function<T, Map<String, Object>> mappingFunction;

  public IteratorDataSource(Iterator<T> iterator, Function<T, Map<String, Object>> mappingFunction){
    this.iter = iterator;
    this.mappingFunction = mappingFunction;
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public Map<String, Object> next() {
    return mappingFunction.apply(iter.next());
  }
}
