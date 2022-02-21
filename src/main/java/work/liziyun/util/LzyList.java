package work.liziyun.util;




import java.lang.reflect.Array;
import java.util.*;

public class LzyList<A> extends AbstractCollection<A> implements java.util.List<A> {
    public A head;
    public LzyList<A> tail;
    private static LzyList<?> EMPTY_LIST = new LzyList<Object>((Object)null, (LzyList)null) {
        public LzyList<Object> setTail(LzyList<Object> var1) {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty() {
            return true;
        }
    };
    private static Iterator<?> EMPTYITERATOR = new Iterator<Object>() {
        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    LzyList(A var1, LzyList<A> var2) {
        this.tail = var2;
        this.head = var1;
    }

    public static <A> LzyList<A> nil() {
        return (LzyList<A>) EMPTY_LIST;
    }

    public static <A> LzyList<A> of(A var0) {
        return new LzyList(var0, nil());
    }

    public static <A> LzyList<A> of(A var0, A var1) {
        return new LzyList(var0, of(var1));
    }

    public static <A> LzyList<A> of(A var0, A var1, A var2) {
        return new LzyList(var0, of(var1, var2));
    }

    public static <A> LzyList<A> of(A var0, A var1, A var2, A... var3) {
        return new LzyList(var0, new LzyList(var1, new LzyList(var2, from(var3))));
    }

    public static <A> LzyList<A> from(A[] var0) {
        LzyList var1 = nil();
        if (var0 != null) {
            for(int var2 = var0.length - 1; var2 >= 0; --var2) {
                var1 = new LzyList(var0[var2], var1);
            }
        }

        return var1;
    }

    /** @deprecated */
    @Deprecated
    public static <A> LzyList<A> fill(int var0, A var1) {
        LzyList var2 = nil();

        for(int var3 = 0; var3 < var0; ++var3) {
            var2 = new LzyList(var1, var2);
        }

        return var2;
    }

    public boolean isEmpty() {
        return this.tail == null;
    }

    public boolean nonEmpty() {
        return this.tail != null;
    }

    public int length() {
        LzyList var1 = this;

        int var2;
        for(var2 = 0; var1.tail != null; ++var2) {
            var1 = var1.tail;
        }

        return var2;
    }

    public int size() {
        return this.length();
    }

    public LzyList<A> setTail(LzyList<A> var1) {
        this.tail = var1;
        return var1;
    }

    public LzyList<A> prepend(A var1) {
        return new LzyList(var1, this);
    }

    public LzyList<A> prependList(LzyList<A> var1) {
        if (this.isEmpty()) {
            return var1;
        } else if (var1.isEmpty()) {
            return this;
        } else if (var1.tail.isEmpty()) {
            return this.prepend(var1.head);
        } else {
            LzyList var2 = this;
            LzyList var3 = var1.reverse();
            LzyAssert.check(var3 != var1);

            while(var3.nonEmpty()) {
                LzyList var4 = var3;
                var3 = var3.tail;
                var4.setTail(var2);
                var2 = var4;
            }

            return var2;
        }
    }

    public LzyList<A> reverse() {
        if (!this.isEmpty() && !this.tail.isEmpty()) {
            LzyList var1 = nil();

            for(LzyList var2 = this; var2.nonEmpty(); var2 = var2.tail) {
                var1 = new LzyList(var2.head, var1);
            }

            return var1;
        } else {
            return this;
        }
    }

    public LzyList<A> append(A var1) {
        return of(var1).prependList(this);
    }

    public LzyList<A> appendList(LzyList<A> var1) {
        return var1.prependList(this);
    }

    public LzyList<A> appendList(LzyListBuffer<A> var1) {
        return this.appendList(var1.toList());
    }

    public <T> T[] toArray(T[] var1) {
        int var2 = 0;
        LzyList var3 = this;

        for(Object[] var4 = var1; var3.nonEmpty() && var2 < var1.length; ++var2) {
            var4[var2] = var3.head;
            var3 = var3.tail;
        }

        if (var3.isEmpty()) {
            if (var2 < var1.length) {
                var1[var2] = null;
            }

            return var1;
        } else {
            var1 = (T[]) Array.newInstance(var1.getClass().getComponentType(), this.size());
            return this.toArray(var1);
        }
    }

    public Object[] toArray() {
        return this.toArray(new Object[this.size()]);
    }

    public String toString(String var1) {
        if (this.isEmpty()) {
            return "";
        } else {
            StringBuffer var2 = new StringBuffer();
            var2.append(this.head);

            for(LzyList var3 = this.tail; var3.nonEmpty(); var3 = var3.tail) {
                var2.append(var1);
                var2.append(var3.head);
            }

            return var2.toString();
        }
    }

    public String toString() {
        return this.toString(",");
    }

    public int hashCode() {
        LzyList var1 = this;

        int var2;
        for(var2 = 1; var1.tail != null; var1 = var1.tail) {
            var2 = var2 * 31 + (var1.head == null ? 0 : var1.head.hashCode());
        }

        return var2;
    }

    public boolean equals(Object var1) {
        if (var1 instanceof List) {
            return equals(this, (LzyList)var1);
        } else if (!(var1 instanceof java.util.List)) {
            return false;
        } else {
            LzyList var2 = this;

            Iterator var3;
            for(var3 = ((java.util.List)var1).iterator(); var2.tail != null && var3.hasNext(); var2 = var2.tail) {
                Object var4 = var3.next();
                if (var2.head == null) {
                    if (var4 != null) {
                        return false;
                    }
                } else if (!var2.head.equals(var4)) {
                    return false;
                }
            }

            return var2.isEmpty() && !var3.hasNext();
        }
    }

    public static boolean equals(LzyList<?> var0, LzyList<?> var1) {
        while(var0.tail != null && var1.tail != null) {
            if (var0.head == null) {
                if (var1.head != null) {
                    return false;
                }
            } else if (!var0.head.equals(var1.head)) {
                return false;
            }

            var0 = var0.tail;
            var1 = var1.tail;
        }

        return var0.tail == null && var1.tail == null;
    }

    public boolean contains(Object var1) {
        for(LzyList var2 = this; var2.tail != null; var2 = var2.tail) {
            if (var1 == null) {
                if (var2.head == null) {
                    return true;
                }
            } else if (var2.head.equals(var1)) {
                return true;
            }
        }

        return false;
    }

    public A last() {
        A var1 = null;

        for(LzyList<A> var2 = this; var2.tail != null; var2 = var2.tail) {
            var1 = var2.head;
        }

        return var1;
    }

    public static <T> LzyList<T> convert(Class<T> var0, LzyList<T> var1) {
        if (var1 == null) {
            return null;
        } else {
            Iterator var2 = var1.iterator();

            while(var2.hasNext()) {
                Object var3 = var2.next();
                var0.cast(var3);
            }

            return var1;
        }
    }

    private static <A> Iterator<A> emptyIterator() {
        return (Iterator<A>) EMPTYITERATOR;
    }

    public Iterator<A> iterator() {
        return this.tail == null ? emptyIterator() : new Iterator<A>() {
            LzyList<A> elems = LzyList.this;

            public boolean hasNext() {
                return this.elems.tail != null;
            }

            public A next() {
                if (this.elems.tail == null) {
                    throw new NoSuchElementException();
                } else {
                    A var1 = this.elems.head;
                    this.elems = this.elems.tail;
                    return var1;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public A get(int var1) {
        if (var1 < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(var1));
        } else {
            LzyList<A> var2 = this;

            for(int var3 = var1; var3-- > 0 && !var2.isEmpty(); var2 = var2.tail) {
            }

            if (var2.isEmpty()) {
                throw new IndexOutOfBoundsException("Index: " + var1 + ", " + "Size: " + this.size());
            } else {
                return var2.head;
            }
        }
    }

    public boolean addAll(int var1, Collection<? extends A> var2) {
        if (var2.isEmpty()) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public A set(int var1, A var2) {
        throw new UnsupportedOperationException();
    }

    public void add(int var1, A var2) {
        throw new UnsupportedOperationException();
    }

    public A remove(int var1) {
        throw new UnsupportedOperationException();
    }

    public int indexOf(Object var1) {
        int var2 = 0;
        LzyList var3 = this;

        while(true) {
            if (var3.tail == null) {
                return -1;
            }

            if (var3.head == null) {
                if (var1 == null) {
                    break;
                }
            } else if (var3.head.equals(var1)) {
                break;
            }

            var3 = var3.tail;
            ++var2;
        }

        return var2;
    }

    public int lastIndexOf(Object var1) {
        int var2 = -1;
        int var3 = 0;

        for(LzyList var4 = this; var4.tail != null; ++var3) {
            label18: {
                if (var4.head == null) {
                    if (var1 != null) {
                        break label18;
                    }
                } else if (!var4.head.equals(var1)) {
                    break label18;
                }

                var2 = var3;
            }

            var4 = var4.tail;
        }

        return var2;
    }

    public ListIterator<A> listIterator() {
        return Collections.unmodifiableList(new ArrayList(this)).listIterator();
    }

    public ListIterator<A> listIterator(int var1) {
        return Collections.unmodifiableList(new ArrayList(this)).listIterator(var1);
    }

    public java.util.List<A> subList(int var1, int var2) {
        if (var1 >= 0 && var2 <= this.size() && var1 <= var2) {
            ArrayList var3 = new ArrayList(var2 - var1);
            int var4 = 0;

            for(LzyList var5 = this; var5.tail != null && var4 != var2; ++var4) {
                if (var4 >= var1) {
                    var3.add(var5.head);
                }

                var5 = var5.tail;
            }

            return Collections.unmodifiableList(var3);
        } else {
            throw new IllegalArgumentException();
        }
    }


}
