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
import java.util.concurrent.locks.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class LockManager<K extends Comparable<K>>
{
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(LockManager.class);

    private class MyReentrantLock extends ReentrantLock
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected Thread getOwner()
        {
            return super.getOwner();
        }
    }

    private Map<K, MyReentrantLock> locks = new HashMap<K, MyReentrantLock>();
    private Map<Long, Integer> heldLocksByThread = new HashMap<Long, Integer>();

    private MyReentrantLock globalLock = new MyReentrantLock();

    public void lockAll()
    {
        globalLock.lock();

        long id = Thread.currentThread().getId();

        // We wait for everyone to get out
        while (true)
        {
            synchronized(locks)
            {
                // if there are no locks or all the locks are ours - then we are OK

                if ((heldLocksByThread.size() == 0) || 
                    ((heldLocksByThread.size() == 1) && (heldLocksByThread.get(id) != null)))
                {
                    break;
                }

                logger.info("tick - waiting for global lock");
                Toolbox.wait(locks, 5000);
            }
        }
    }

    public void unlockAll()
    { 
        globalLock.unlock();
    }

    public void lock(K key)
    {
        List<K> keys = new ArrayList<K>();
        keys.add(key);
        lock(keys);
    }

    public void lock(List<K> keys)
    {
        long id = Thread.currentThread().getId();

        // If we are holding locks we are allowed to increase our holding
        // We can only take a first lock if no-one is asking for the globalLock

        boolean isFirstLock = false;
        synchronized(locks)
        {
            if (heldLocksByThread.get(id) == null)
            {
                isFirstLock = true;
            }
        }

        if (isFirstLock)
        {
            globalLock.lock();
            synchronized(locks)
            {
                heldLocksByThread.put(id, keys.size());
            }
            globalLock.unlock();
        }
        else
        {
            synchronized(locks)
            {
                heldLocksByThread.put(id, heldLocksByThread.get(id) + keys.size());
            }
        }

        // We need to sort so that the lock acquisition order is the same
        Collections.sort(keys, new Comparator<K>(){public int compare(K id1, K id2){return id1.compareTo(id2);}});

        // We must not hold synchronized(locks) while we lock the lock - think about it
        for (K key: keys)
        {
            MyReentrantLock lock;
            synchronized(locks)
            {
                lock = locks.get(key);
                if (lock == null)
                {
                    lock = new MyReentrantLock();
                    locks.put(key, lock);
                }
            }

            lock.lock();
        }
    }

    public void unlock(K key)
    {
        List<K> keys = new ArrayList<K>();
        keys.add(key);
        unlock(keys);
    }

    public void unlock(List<K> keys)
    {
        synchronized(locks)
        {           
            for (K key: keys)
            {
                ReentrantLock lock = locks.get(key);
                if (lock == null)
                    throw new RuntimeException("Lock not created - key=" + key);

                lock.unlock();
            }
        }

        synchronized(locks)
        {
            long id = Thread.currentThread().getId();
            if (heldLocksByThread.get(id) == keys.size())
                heldLocksByThread.remove(id);
            else
                heldLocksByThread.put(id, heldLocksByThread.get(id) - keys.size());

            // kick anyone waiting in lockAll()
            locks.notify();
        }
    }

    public boolean isLocked(K key)
    {
        synchronized(locks)
        {
            if (globalLock.isHeldByCurrentThread())
                return true;
            ReentrantLock lock = locks.get(key);
            if ((lock != null) && lock.isHeldByCurrentThread())
                return true;
            return false;
        }
    }

    public boolean isLocked()
    {
        synchronized(locks)
        {
            if (globalLock.isHeldByCurrentThread())
                return true;
            return false;
        }
    }

    public int getHeldLocks()
    {
        synchronized(locks)
        {
            int total = 0;
            for (int heldLocks: heldLocksByThread.values())
                total += heldLocks;
            return total;
        }
    }

    public void dump()
    {
        dump(null);
    }

    public void dump(StringBuilder buffer)
    {
        synchronized(locks)
        {
            if (globalLock.isLocked())
            {
               Thread thread = globalLock.getOwner();
               String text = "globalLock is held by thread=" + (thread == null ? "null" : thread.getName());
               if (buffer == null)
                   logger.info(text);
               else
                   buffer.append(text).append("\n");
            }
            for (K key: locks.keySet())
            {
               MyReentrantLock lock = locks.get(key);
               if (! lock.isLocked())
                   continue;
               // no-one can unlock this lock as we hold synchronized(locks)
               String text = "lock key=" + key + " is held by thread=" + lock.getOwner().getName();
               if (buffer == null)
                   logger.info(text);
               else
                   buffer.append(text).append("\n");
            }
        }
    }
}
