/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util.persistencefn;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.P2;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;

import org.joda.time.base.AbstractInstant;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 * JPA query constructors.
 */
public final class Queries {
  private Queries() {
  }

  /** {@link javax.persistence.EntityManager#find(Class, Object)} as a function wrapping the result into an Option. */
  public static <A> Fn<EntityManager, Opt<A>> find(final Class<A> clazz, final Object primaryKey) {
    return new Fn<EntityManager, Opt<A>>() {
      @Override public Opt<A> apply(EntityManager em) {
        return Opt.nul(em.find(clazz, primaryKey));
      }
    };
  }

  /** {@link javax.persistence.EntityManager#persist(Object)} as a function. */
  public static <A> Fn<EntityManager, A> persist(final A a) {
    return new Fn<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        em.persist(a);
        return a;
      }
    };
  }

  /**
   * {@link javax.persistence.EntityManager#contains(Object)} as a function.
   * The function returns true if the entity is a managed instance belonging to the current persistence context.
   */
  public static Fn<EntityManager, Boolean> contains(final Object a) {
    return new Fn<EntityManager, Boolean>() {
      @Override public Boolean apply(EntityManager em) {
        return em.contains(a);
      }
    };
  }

  /**
   * Convenience for <code>EntityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(a)</code>.
   * The function returns the ID or null if the object does not have an ID yet, i.e. is not yet persisted.
   */
  public static Object getId(EntityManager em , Object a) {
    return em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(a);
  }

  /**
   * {@link #getId(EntityManager, Object)} as a function.
   */
  @SuppressWarnings("unchecked")
  public static <A> Fn<EntityManager, A> getId(final Object a) {
    return new Fn<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        return (A) getId(em, a);
      }
    };
  }

  /**
   * Like {@link javax.persistence.EntityManager#remove(Object)} but the entity is allowed
   * to be detached. It will be merged into the current persistence context if necessary.
   */
  public static void remove(EntityManager em, final Object a) {
    if (em.contains(a)) {
      em.remove(a);
    } else {
      em.remove(em.merge(a));
    }
  }

  /** {@link #remove(EntityManager, Object)} as a function. */
  public static Fn<EntityManager, Unit> remove(final Object a) {
    return new Fn<EntityManager, Unit>() {
      @Override public Unit apply(EntityManager em) {
        remove(em, a);
        return Unit.unit;
      }
    };
  }

  /**
   * If the object does not have an ID, persist it. If it does, try to update it.
   */
  public static <A> A persistOrUpdate(final EntityManager em, final A a) {
    final Object id = getId(em, a);
    if (id == null) {
      // object does not have an ID
      em.persist(a);
      return a;
    } else {
      @SuppressWarnings("unchecked")
      final A dto = (A) em.find(a.getClass(), id);
      if (dto == null) {
        em.persist(a);
        return a;
      } else {
      return em.merge(a);
      }
    }
  }

  /**
   * {@link #persistOrUpdate(EntityManager, Object)} as a function.
   */
  public static <A> Fn<EntityManager, A> persistOrUpdate(final A a) {
    return new Fn<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        return persistOrUpdate(em, a);
      }
    };
  }

  /**
   * Set a list of named parameters on a query.
   * <p>
   * Values of type {@link java.util.Date} and {@link org.joda.time.base.AbstractInstant}
   * are recognized and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
   */
  public static <A extends Query> A setParams(A q, P2<String, ?>... params) {
    for (P2<String, ?> p : params) {
      final Object value = p.get2();
      if (value instanceof Date) {
        q.setParameter(p.get1(), (Date) value, TemporalType.TIMESTAMP);
      }
      if (value instanceof AbstractInstant) {
        q.setParameter(p.get1(), ((AbstractInstant) value).toDate(), TemporalType.TIMESTAMP);
      } else {
        q.setParameter(p.get1(), p.get2());
      }
    }
    return q;
  }

  /**
   * Set a list of positional parameters on a query.
   * <p>
   * Values of type {@link java.util.Date} and {@link org.joda.time.base.AbstractInstant}
   * are recognized and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
   */
  public static <A extends Query> A setParams(A q, Object... params) {
    for (int i = 0; i < params.length; i++) {
      final Object value = params[i];
      if (value instanceof Date) {
        q.setParameter(i + 1, (Date) value, TemporalType.TIMESTAMP);
      }
      if (value instanceof AbstractInstant) {
        q.setParameter(i + 1, ((AbstractInstant) value).toDate(), TemporalType.TIMESTAMP);
      } else {
        q.setParameter(i + 1, value);
      }
    }
    return q;
  }

  // -------------------------------------------------------------------------------------------------------------------

  /** Named queries with support for named parameters. */
  public static final TypedQueriesBase<P2<String, ?>> named = new TypedQueriesBase<P2<String, ?>>() {
    @Override public Query query(EntityManager em, String q, P2<String, ?>... params) {
      return setParams(em.createNamedQuery(q), params);
    }
  };

  /** Native SQL queries. Only support positional parameters. */
  public static final QueriesBase<Object> sql = new QueriesBase<Object>() {
    @Override public Query query(EntityManager em, String q, Object... params) {
      return setParams(em.createNativeQuery(q), params);
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public abstract static class TypedQueriesBase<P> extends QueriesBase<P> {
    protected TypedQueriesBase() {
    }

  }

  // -------------------------------------------------------------------------------------------------------------------

  public abstract static class QueriesBase<P> {
    protected QueriesBase() {
    }

    /**
     * Create a query from <code>q</code> with a list of parameters.
     * Values of type {@link java.util.Date} are recognized
     * and set as a timestamp ({@link javax.persistence.TemporalType#TIMESTAMP}.
     */
    public abstract Query query(EntityManager em, String q, P... params);

    /** Run an update (UPDATE or DELETE) query and ensure that at least one row got affected. */
    public boolean update(EntityManager em, String q, P... params) {
      return query(em, q, params).executeUpdate() > 0;
    }

    /**
     * Run a SELECT query that should return a single result.
     *
     * @return some value if the query yields exactly one result, none otherwise
     */
    public <A> Opt<A> findSingle(final EntityManager em,
                                 final String q,
                                 final P... params) {
      try {
        return Opt.some((A) query(em, q, params).getSingleResult());
      } catch (NoResultException e) {
        return Opt.none();
      } catch (NonUniqueResultException e) {
        return Opt.none();
      }
    }

    /** {@link #findSingle(javax.persistence.EntityManager, String, Object[])} as a function. */
    public <A> Fn<EntityManager, Opt<A>> findSingle(final String q,
                                                    final P... params) {
      return new Fn<EntityManager, Opt<A>>() {
        @Override public Opt<A> apply(EntityManager em) {
          return findSingle(em, q, params);
        }
      };
    }

    /** Find multiple entities. */
    public <A> List<A> findAll(EntityManager em, final String q, final P... params) {
      return (List<A>) query(em, q, params).getResultList();
    }

    /** {@link #findAll(javax.persistence.EntityManager, String, Object[])} as a function. */
    public <A> Fn<EntityManager, List<A>> findAll(final String q,
                                                  final P... params) {
      return new Fn<EntityManager, List<A>>() {
        @Override public List<A> apply(EntityManager em) {
          return findAll(em, q, params);
        }
      };
    }

  }
}
