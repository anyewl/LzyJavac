package work.liziyun.util;



import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LzyListBuffer<A> extends AbstractQueue<A> {
    public LzyList<A> elems;
    public LzyList<A> last;
    public int count;
    public boolean shared;

    public static <T> LzyListBuffer<T> lb() {
        return new LzyListBuffer();
    }

    public static <T> LzyListBuffer<T> of(T var0) {
        LzyListBuffer var1 = new LzyListBuffer();
        var1.add(var0);
        return var1;
    }

    public LzyListBuffer() {
        this.clear();
    }

    public final void clear() {
        this.elems = new LzyList((Object)null, (LzyList)null);
        this.last = this.elems;
        this.count = 0;
        this.shared = false;
    }

    public int length() {
        return this.count;
    }

    public int size() {
        return this.count;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public boolean nonEmpty() {
        return this.count != 0;
    }

    private void copy() {
        LzyList var1 = this.elems = new LzyList(this.elems.head, this.elems.tail);

        while(true) {
            LzyList var2 = var1.tail;
            if (var2 == null) {
                this.last = var1;
                this.shared = false;
                return;
            }

            var2 = new LzyList(var2.head, var2.tail);
            var1.setTail(var2);
            var1 = var2;
        }
    }

    public LzyListBuffer<A> prepend(A var1) {
        this.elems = this.elems.prepend(var1);
        ++this.count;
        return this;
    }

    public LzyListBuffer<A> append(A var1) {
        var1.getClass();
        if (this.shared) {
            this.copy();
        }

        this.last.head = var1;
        this.last.setTail(new LzyList((Object)null, (LzyList)null));
        this.last = this.last.tail;
        ++this.count;
        return this;
    }

    public LzyListBuffer<A> appendList(LzyList<A> var1) {
        while(var1.nonEmpty()) {
            this.append(var1.head);
            var1 = var1.tail;
        }

        return this;
    }

    public LzyListBuffer<A> appendList(LzyListBuffer<A> var1) {
        return this.appendList(var1.toList());
    }

    public LzyListBuffer<A> appendArray(A[] var1) {
        for(int var2 = 0; var2 < var1.length; ++var2) {
            this.append(var1[var2]);
        }

        return this;
    }

    public LzyList<A> toList() {
        this.shared = true;
        return this.elems;
    }

    public boolean contains(Object var1) {
        return this.elems.contains(var1);
    }

    public <T> T[] toArray(T[] var1) {
        return this.elems.toArray(var1);
    }

    public Object[] toArray() {
        return this.toArray(new Object[this.size()]);
    }

    public A first() {
        return this.elems.head;
    }

    public A next() {
        A var1 = this.elems.head;
        if (this.elems != this.last) {
            this.elems = this.elems.tail;
            --this.count;
        }

        return var1;
    }

    public Iterator<A> iterator() {
        return new Iterator<A>() {
            LzyList<A> elems;

            {
                this.elems = LzyListBuffer.this.elems;
            }

            public boolean hasNext() {
                return this.elems != LzyListBuffer.this.last;
            }

            public A next() {
                if (this.elems == LzyListBuffer.this.last) {
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

    public boolean add(A var1) {
        this.append(var1);
        return true;
    }

    public boolean remove(Object var1) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> var1) {
        Iterator var2 = var1.iterator();

        Object var3;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            var3 = var2.next();
        } while(this.contains(var3));

        return false;
    }

    public boolean addAll(Collection<? extends A> var1) {
        Iterator var2 = var1.iterator();

        while(var2.hasNext()) {
            A var3 = (A) var2.next();
            this.append(var3);
        }

        return true;
    }

    public boolean removeAll(Collection<?> var1) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> var1) {
        throw new UnsupportedOperationException();
    }

    public boolean offer(A var1) {
        this.append(var1);
        return true;
    }

    public A poll() {
        return this.next();
    }

    public A peek() {
        return this.first();
    }
}
