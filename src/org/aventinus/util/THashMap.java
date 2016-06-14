//-----------------------------------------------------------------------------------
// Copyright (c) 2009-2013, Gordon Beck (gordon.beck@aventinus.org). All rights reserved.
//
//    This file is part of a suite of tools. 
//
//    The tools are free software: you can redistribute it and/or modify 
//    it under the terms of the GNU General Public License as published by 
//    the Free Software Foundation, either version 3 of the License, or 
//    (at your option) any later version. 
// 
//    The tools are distributed in the hope that they will be useful, 
//    but WITHOUT ANY WARRANTY; without even the implied warranty of 
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
//    GNU General Public License for more details. 
// 
//    You should have received a copy of the GNU General Public License 
//    along with these tools.  If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------------
package org.aventinus.util;

import java.util.*;

//-----------------------------------------------------------------------------------
// updates & deletes are keyed by threadId.
// getUpdates()/getDeletes() only return updates done by this thread
// get() only returns items updated by this thread otherwise it returns original
// commit() only commits those returned by getUpdates()/getDeletes()
// put/remove fail if we do not have the lock
//-----------------------------------------------------------------------------------
public class THashMap<K,V> extends HashMap<K,V>
{
    public static final long serialVersionUID = 1;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(THashMap.class);

    private Map<Long, Map<K,V>> updatesByThread = new HashMap<Long, Map<K,V>>();
    private Map<Long, Set<K>> deletesByThread = new HashMap<Long, Set<K>>();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public void begin()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            if ((updatesByThread.get(id) != null) || (deletesByThread.get(id) != null))
            {
                throw new IllegalStateException("Pending updates");
            }
        }
    }

    public void commit()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Map<K,V> updates = updatesByThread.get(id);
            Set<K> deletes = deletesByThread.get(id);

            if (updates != null)
            {
                for (Map.Entry<K, V> entry: updates.entrySet())
                {
                    if ((deletes == null) || (! deletes.contains(entry.getKey())))
                    {
                        if (! isLocked(id, entry.getValue(), castKey(entry.getKey())))
                        {
                            throw new IllegalStateException("Lock not held id=" + id + " key=" + entry.getKey());
                        }
                    }
                }
            }

            if (deletes != null)
            {
                for (Object key: deletes)
                {
                    if (! isLocked(id, super.get(key), castKey(key)))
                    {
                        throw new IllegalStateException("Lock not held id=" + id + " key=" + key);
                    }
                }
            }

            if (updates != null)
            {
                for (Map.Entry<K, V> entry: updates.entrySet())
                {
                    if ((deletes == null) || (! deletes.contains(entry.getKey())))
                    {
                        super.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (deletes != null)
            {
                for (Object key: deletes)
                {
                    super.remove(key);
                }
            }

            updatesByThread.remove(id);
            deletesByThread.remove(id);
        }
    }

    public void rollback()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            updatesByThread.remove(id);
            deletesByThread.remove(id);
        }
    }

    // the default locking assumes that the caller manages locks

    public boolean isLocked(long id)
    {
        return true;
    }

    public boolean isLocked(long id, V value, K key)
    {
        return true;
    }

    @SuppressWarnings("unchecked")
    private K castKey(Object key)
    {
        return (K)key;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    @Override
    public V get(Object key)
    {
        // you do not need the lock to get() a member - but obviously if not locked it can change

        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Set<K> deletes = deletesByThread.get(id);

            if ((deletes != null) && deletes.contains(key))
            {
                return null;
            }

            Map<K,V> updates = updatesByThread.get(id);

            if (updates != null)
            {
                V value = updates.get(key);
                if (value != null)
                {
                    return value;
                }
            }

            return super.get(key);
        }
    }

    @Override
    public V put(K key, V value)
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            if (! isLocked(id, value, key))
            {
                throw new IllegalStateException("Lock not held id=" + id + " key=" + key);
            }

            Map<K,V> updates = updatesByThread.get(id);
            Set<K> deletes = deletesByThread.get(id);

            if (deletes != null)
            {
                deletes.remove(key);
            }

            if (updates == null)
            {
                updates = new HashMap<K, V>();
                updatesByThread.put(id, updates);
            }

            updates.put(key, value);

            return value;
        }
    }

    @Override
    public V remove(Object key)
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Set<K> deletes = deletesByThread.get(id);

            if ((deletes != null) && deletes.contains(key))
            {
                // lock already checked
                return null;
            }

            Map<K,V> updates = updatesByThread.get(id);

            V value;

            if (updates == null)
            {
                value = super.get(key);
            }
            else
            {
                value = updates.remove(key);
                if (value == null)
                {
                    value = super.get(key);
                }
            }

            if (value == null)
            {
                // nothing being deleted(changed) so do not really need lock
                return null;
            }

            if (! isLocked(id, value, castKey(key)))
            {
                throw new IllegalStateException("Lock not held id=" + id + " key=" + key);
            }

            if (deletes == null)
            {
                deletes = new HashSet<K>();
                deletesByThread.put(id, deletes);
            }

            deletes.add(castKey(key));

            return value;
        }
    }

    @Override
    public boolean containsKey(Object key) 
    {
        // you do not need the lock to get() a member - but obviously if not locked it can change

        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Set<K> deletes = deletesByThread.get(id);

            if ((deletes != null) && deletes.contains(key))
            {
                return false;
            }

            Map<K,V> updates = updatesByThread.get(id);

            if (updates != null)
            {
                if (updates.get(key) != null)
                {
                    return true;
                }
            }

            return super.containsKey(key);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public V getOriginal(K key)
    {
        synchronized(this)
        {
            return super.get(key);
        }
    }

    public Map<K,V> getUpdates()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Map<K, V> updates = updatesByThread.get(id);

            if (updates == null)
                return new HashMap<K, V>();
            return updates;
        }
    }

    public Set<K> getDeletes()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();

            Set<K> deletes = deletesByThread.get(id);

            if (deletes == null)
                return new HashSet<K>();
            return deletes;
        }
    } 

    public List<V> committedValues() 
    {
        synchronized(this)
        {
            return new ArrayList<V>(super.values());
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    @Override
    public void clear()
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();
            if (! isLocked(id))
                throw new IllegalStateException("GlobalLock not held");
            if ((updatesByThread.size() > 0) || (deletesByThread.size() > 0))
                throw new RuntimeException("Outstanding transaction(s)");
            super.clear();
        }
    }

    @Override
    public Object clone() 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K,V>> entrySet() 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K,? extends V> m) 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() 
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();
            if (! isLocked(id))
                throw new IllegalStateException("GlobalLock not held");
            if ((updatesByThread.size() > 0) || (deletesByThread.size() > 0))
                throw new RuntimeException("Outstanding transaction(s)");

            return super.size();
        }
    }

    @Override
    public Collection<V> values() 
    {
        synchronized(this)
        {
            long id = Thread.currentThread().getId();
            if (! isLocked(id))
                throw new IllegalStateException("GlobalLock not held");
            if ((updatesByThread.size() > 0) || (deletesByThread.size() > 0))
                throw new RuntimeException("Outstanding transaction(s)");
            return super.values();
        }
    }
}
