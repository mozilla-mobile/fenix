// This is example code for the 'Simplified Example' section of
// /docs/architecture-overview-outline.md


// TODO fill in relevent code here


class ContactsView(
    private val container: ViewGroup,
    private val interactor: ContactsInteractor
) {

    init {
        // setup stuff
    }

    fun update(state: ContactsState) {

    }
}

class ContactsInteractor(
    private val ContactsController,
    private val ThemeController
) {

}

class ContactsController(
    private val ContactsStore,
    private val NavController
) {

}
class ThemeController(
    private val ContactsStore
) {

}

class ContactsStore(
    private val initialState: ContactsState,
    private val reducer: Reducer<?, ?>
) {

}

data class ContactsState(

)

class ContactsFragment : Fragment() {

}
