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

import com.google.re2j.Pattern;
import com.google.re2j.Matcher;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.commons.io.FileUtils;

/**
 * Data source for reading from a file, using a RegEx to match records
 */
public class RegexDataSource implements DataSource{

  private final Matcher matcher;
  private boolean hasNext = false;

  public RegexDataSource(File file, Charset encoding, Pattern pattern) throws IOException {
    this(FileUtils.readFileToString(file, encoding), pattern);
  }

  public RegexDataSource(File file, Pattern pattern) throws IOException {
    this(FileUtils.readFileToString(file, Charset.defaultCharset()), pattern);
  }

  public RegexDataSource(String text, Pattern pattern){
    matcher = pattern.matcher(text);
  }

  @Override
  public boolean hasNext() {
    hasNext = matcher.find();
    return hasNext;
  }

  @Override
  public Map<String, Object> next() {
    if(!hasNext)
      throw new NoSuchElementException();
    
    Map<String, Object> map = new HashMap<>();

    for (int i = 0; i <= matcher.groupCount(); i++){
      map.put(String.valueOf(i), matcher.group(i));
    }

    return map;
  }
}
