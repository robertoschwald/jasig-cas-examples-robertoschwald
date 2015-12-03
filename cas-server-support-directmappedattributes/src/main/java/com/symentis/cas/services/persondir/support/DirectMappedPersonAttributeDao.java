/**
 * Copyright 2015 symentis GmbH
 *
 * Based on the Jasig Person Directory ComplexStubPersonAttributeDao
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.symentis.cas.services.persondir.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao;
import org.jasig.services.persondir.support.AttributeNamedPersonImpl;
import org.jasig.services.persondir.support.IUsernameAttributeProvider;
import org.jasig.services.persondir.support.NamedPersonImpl;
import org.jasig.services.persondir.util.PatternHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A IPersonAttributeDao backed by a single short-term ConcurrentHashMap.
 *
 * The users attributes additionally hold the cache insertion time
 * for later map entry cleanup if e.g. no serviceValidate phase happens
 * (which normally removes the users attributes from the cache).
 *
 * @author: Robert Oschwald
 *
 * Attribute cache entry can be written by the AuthenticationHandler this dao is
 * wired into.
 *
 * Inspired by @see org.jasig.services.persondir.support.ComplexStubPersonAttributeDao
 *
 * Configuration:
 *
 * 1. Define the repository bean
 * <p>
 *   <bean id="attributeRepository" class="com.symentis.cas.services.persondir.support.DirectMappedPersonAttributeDao">
 *       <property name="possibleUserAttributeNames">
 *         <description>defines the user attributes that a service may return</description>
 *         <set>
 *           <value>firstname</value>
 *           <value>lastname</value>
 *           <value>whatever</value>
 *         </set>
 *       </property>
 *       <!-- Optional. Time to live of the cache entries (minutes). Default is 1 minute. -->
 *       <property name="TTL" value="2" />
 *    </bean>
 * </p>
 *
 * 2. Wire the attributeRepository into the resolver, e.g:
 * <bean class="org.jasig.cas.authentication.principal.UsernamePasswordCredentialsToPrincipalResolver" >
 *    <property name="attributeRepository">
 *       <ref bean="attributeRepository" />
 *    </property>
 * </bean>
 *
 * 3. Wire the bean into the authenticationHandler. The Handler implementation must be able to receive the dao and
 *    also needs to set the principals attributes (e.g. in authenticateUsernamePasswordInternal())
 * <p>
 *   <bean id="myAuthHandler" class="com.symentis.cas.services.example.WebserviceAuthenticationHandler">
 *       <property name="webserviceClient" ref="myAuthWsClient"/>
 *       <property name="attributeRepository" ref="attributeRepository"/>
 *   </bean>
 * </p>
 */
public class DirectMappedPersonAttributeDao extends AbstractQueryPersonAttributeDao<String> {
  public static final String ATTRIBUTE_CACHE_EXPIRY_TIME_KEY = "USER_ATTRIB_CACHE_EXPIRY_TIME";
  private static Log log = LogFactory.getLog(DirectMappedPersonAttributeDao.class);
  /* Cached Attribute Map for users. Can be updated by AuthenticationHandler this Dao is wired into */
  private ConcurrentHashMap<String, Map<String, List<Object>>> backingMap;
  private long DEFAULT_CACHE_ENTRY_TTL = MILLISECONDS.convert(1, MINUTES);
  private long ttl = DEFAULT_CACHE_ENTRY_TTL;
  private Set<String> possibleUserAttributeNames = Collections.emptySet();
  private String queryAttributeName = null;

  /* Constructor. */
  public DirectMappedPersonAttributeDao() {
    this.backingMap = new ConcurrentHashMap<String, Map<String, List<Object>>>();
  }

  /**
   * Constructor.
   * Creates a new, empty user backingMap with the specified initial capacity, load
   * factor, and concurrency level.
   *
   * @param initialCapacity  - the initial capacity. The implementation
   *                         performs internal sizing to accommodate this many elements.
   * @param loadFactor       - the load factor threshold, used to control resizing.
   *                         Resizing may be performed when the average number of elements per bin
   *                         exceeds this threshold.
   * @param concurrencyLevel - the estimated number of concurrently updating
   *                         threads. The implementation performs internal sizing to try to
   *                         accommodate this many threads.
   */
  public DirectMappedPersonAttributeDao(String queryAttributeName, final int initialCapacity, final float loadFactor, final int concurrencyLevel) {
    this.backingMap = new ConcurrentHashMap<String, Map<String, List<Object>>>(initialCapacity, loadFactor, concurrencyLevel);
    this.setQueryAttributeName(queryAttributeName);
  }

  /*
   * Sets the time-to-live for cached attribute entries.
   * When the cached attribute expires, it will be forcibly removed from the cache.
  */
  public void setTTL(final int minutes) {
    if (minutes < 1) {
      throw new IllegalArgumentException("TTL must be a positive int value");
    }
    this.ttl = MILLISECONDS.convert(minutes, MINUTES);
  }

  public String getQueryAttributeName() {
    return this.queryAttributeName;
  }

  /**
   * Name of the attribute to look for to key into the backing map. If not set the value returned by
   * {@link #getUsernameAttributeProvider()} will be used.
   */
  public void setQueryAttributeName(String queryAttributeName) {
    this.queryAttributeName = queryAttributeName;
  }

  /* (non-Javadoc)
 * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getPossibleUserAttributeNames()
 */
  @Override
  public Set<String> getPossibleUserAttributeNames() {
    return this.possibleUserAttributeNames;
  }

  public void setPossibleUserAttributeNames(Set<String> possibleUserAttributeNames) {
    this.possibleUserAttributeNames = possibleUserAttributeNames;
  }

  /* (non-Javadoc)
  * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getAvailableQueryAttributes()
  */
  @Override
  public Set<String> getAvailableQueryAttributes() {
    final IUsernameAttributeProvider usernameAttributeProvider = this.getUsernameAttributeProvider();
    final String usernameAttribute = usernameAttributeProvider.getUsernameAttribute();
    return Collections.singleton(usernameAttribute);
  }

  /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#appendAttributeToQuery(java.lang.Object, java.lang.String, java.util.List)
     */
  @Override
  protected String appendAttributeToQuery(String queryBuilder, String dataAttribute, List<Object> queryValues) {
    if (queryBuilder != null) {
      return queryBuilder;
    }

    final String keyAttributeName;
    if (this.queryAttributeName != null) {
      keyAttributeName = this.queryAttributeName;
    } else {
      final IUsernameAttributeProvider usernameAttributeProvider = this.getUsernameAttributeProvider();
      keyAttributeName = usernameAttributeProvider.getUsernameAttribute();
    }

    if (keyAttributeName.equals(dataAttribute)) {
      return String.valueOf(queryValues.get(0));
    }

    return null;
  }

  /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getPeopleForQuery(java.lang.Object, java.lang.String)
     */
  @Override
  protected List<IPersonAttributes> getPeopleForQuery(String seedValue, String queryUserName) {
    if (seedValue != null && seedValue.contains(IPersonAttributeDao.WILDCARD)) {
      final Pattern seedPattern = PatternHelper.compilePattern(seedValue);
      final List<IPersonAttributes> results = new LinkedList<IPersonAttributes>();
      for (final Map.Entry<String, Map<String, List<Object>>> attributesEntry : this.backingMap.entrySet()) {
        final String attributesKey = attributesEntry.getKey();
        final Matcher keyMatcher = seedPattern.matcher(attributesKey);
        if (keyMatcher.matches()) {
          final Map<String, List<Object>> attributes = attributesEntry.getValue();
          if (attributes != null) {
            final IPersonAttributes person = this.createPerson(null, queryUserName, attributes);
            results.add(person);
            if (log.isDebugEnabled()) {
              log.debug("Found attributes for netid " + queryUserName);
            }
          }
        }
      }
      if (results.size() == 0) {
        return null;
      }
      return results;
    }
    // no wildcard in seedValue. Use straight seedValue to get the attribute from the backingMap
    final Map<String, List<Object>> attributes = this.backingMap.remove(seedValue);
    if (attributes == null) {
      return null;
    }
    if (log.isDebugEnabled()) {
      log.debug("Obtained attributes from cache for netid " + queryUserName);
      log.debug("Attribute enries remaining in cache: " + this.backingMap.size());
    }
    final IPersonAttributes person = this.createPerson(seedValue, queryUserName, attributes);
    return Collections.singletonList(person);
  }

  private IPersonAttributes createPerson(String seedValue, String queryUserName, Map<String, List<Object>> attributes) {
    //
    final IPersonAttributes person;
    if (queryUserName != null) {
      // Option #1:  Use the userName attribute provided in the query
      person = new NamedPersonImpl(queryUserName, attributes);
    } else {
      // Option #2:  Create the IPersonAttributes doing a best-guess
      final String usernameAttribute = this.getConfiguredUserNameAttribute();
      if (seedValue != null && usernameAttribute.equals(this.queryAttributeName)) {
        person = new NamedPersonImpl(seedValue, attributes);
      } else {
        person = new AttributeNamedPersonImpl(usernameAttribute, attributes);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("Created Person:" + person);
    }
    return person;
  }

  /** Add attributes to the internal cache for netid **/
  public void addAttributes(String netid, Map<String, List<Object>> attributes) {
    cleanupCache();
    // for later entry cleanup to avoid unlimited growth.
    ArrayList<Object> attributeExpiryTime = new ArrayList<Object>();
    attributeExpiryTime.add(new Date().getTime() + ttl);
    attributes.put(ATTRIBUTE_CACHE_EXPIRY_TIME_KEY, attributeExpiryTime);
    //
    Map<String, List<Object>> existing = this.backingMap.putIfAbsent(netid, attributes);
    if (existing != null) {
      // replace pre-existing entry
      this.backingMap.replace(netid, attributes);
    }
    if (log.isDebugEnabled()) {
      log.debug("Stored attributes for netid: " + netid + " :" + attributes);
    }
  }

  /* removes expired attribute entries from cache.
     This may occur when authentication is performed but no ticket validation happens.
  */
  private void cleanupCache() {
    Iterator<Map.Entry<String, Map<String, List<Object>>>> iterator = this.backingMap.entrySet().iterator();
    if (log.isDebugEnabled()){
      log.debug("Entries in attribute cache: " + this.backingMap.size());
    }
    while (iterator.hasNext()) {
      Map.Entry<String, Map<String, List<Object>>> entry = iterator.next();
      Map<String, List<Object>> attribute = entry.getValue();
      if (!attribute.containsKey(ATTRIBUTE_CACHE_EXPIRY_TIME_KEY)) return;
      List<Object> cacheInsertMapValues = attribute.get(ATTRIBUTE_CACHE_EXPIRY_TIME_KEY);
      if (cacheInsertMapValues.size() < 1) {
        // empty attribute value list
        log.warn("Attribute entry in cache has no values. Removing.");
        iterator.remove();
        continue;
      }
      Date expiryDate = new Date((Long)cacheInsertMapValues.get(0));
      if (expiryDate == null) {
        log.warn("Cannot obtain cache expiry date for netid" + entry.getKey());
        continue;
      }
      if (new Date().after(expiryDate)) {
        if (log.isDebugEnabled()) {
          log.debug("Removing expired attributes from cache for netid: " + entry.getKey());
        }
        iterator.remove();
      }
    }
  }
}

