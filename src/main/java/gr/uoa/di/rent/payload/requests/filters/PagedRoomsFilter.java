package gr.uoa.di.rent.payload.requests.filters;

public class PagedRoomsFilter extends PagedResponseFilter {


    public PagedRoomsFilter() {
        super();
    }

    public PagedRoomsFilter(int page, int size, String sort_field, String order) {
        super(page, size, sort_field, order);
    }
}
