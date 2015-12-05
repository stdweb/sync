package stdweb.Repository

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class EntityQueryRange : Pageable
{
    constructor(top : Int, offset : Int,pageSize : Int, _sort : Sort)
    {
        this.startIndex=top-offset;
        this.endIndex=pageSize+this.startIndex
        sort1=_sort

    }
    constructor(_start : Int, _end : Int, _sort : Sort)
    {
        this.endIndex=_end
        this.startIndex=_start
        this.sort1=_sort
    }
    var startIndex : Int
    var endIndex : Int
    var sort1 : Sort


    override fun getPageNumber(): Int {
        throw UnsupportedOperationException()
    }

    override fun hasPrevious(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getSort(): Sort? {
        return this.sort1
    }

    override fun next(): Pageable? {
        throw UnsupportedOperationException()
    }

    override fun getPageSize(): Int {
        if (endIndex == 0)
            return 0

        return endIndex - startIndex;

    }

    override fun getOffset(): Int {
        return startIndex;
        //return endIndex
    }

    override fun first(): Pageable? {
        throw UnsupportedOperationException()
    }

    override fun previousOrFirst(): Pageable? {
        throw UnsupportedOperationException()
    }

}