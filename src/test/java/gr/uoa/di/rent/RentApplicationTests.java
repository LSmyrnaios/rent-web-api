package gr.uoa.di.rent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gr.uoa.di.rent.exceptions.AppException;
import gr.uoa.di.rent.models.*;
import gr.uoa.di.rent.payload.requests.LoginRequest;
import gr.uoa.di.rent.repositories.*;
import gr.uoa.di.rent.repositories.HotelRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

//import java.time.LocalDate;
import java.util.*;
//import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(classes = {Application.class})
public class RentApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(RentApplicationTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AmenitiesRepository amenitiesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext wac;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void loginAsAdmin() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String s = gson.toJson(new LoginRequest("admin@rentcube.com", "asdfk2.daADd"));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s)
        ).andDo(print()).andExpect(status().isOk());
    }

    @Test
    public void insertUser() {

        // Insert the user if not exist.
        if (userRepository.findByEmail("user@mail.com").isPresent())
            return;

        // Assign an user role
        Role role = roleRepository.findByName(RoleName.ROLE_USER);
        if (role == null) {
            throw new AppException("User Role not set.");
        }

        User user = new User(
                null,
                "user",
                passwordEncoder.encode("asdfk2.daADd"),
                "user@mail.com",
                role,
                false,
                false,
                null,
                null
        );

        String name = "Simple";
        String surname = "User";
        Profile profile = new Profile(
                user,
                name,
                surname,
                new Date(),
                "https://ui-avatars.com/api/?name=" + name + "+" + surname + "&rounded=true&%20bold=true&" +
                        "background=a8d267&color=000000"
        );
        user.setProfile(profile);

        // Create wallet
        Wallet wallet = new Wallet(user, 1000.00);
        user.setWallet(wallet);

        userRepository.save(user);
    }

    @Test
    public void insertProvider() {

        // Insert provider (with a business and two hotels, each hotel having 3 rooms)

        // Insert the provider if not exist.
        if (userRepository.findByEmail("provider@mail.com").isPresent())
            return;

        // Assign an provider role
        Role role = roleRepository.findByName(RoleName.ROLE_PROVIDER);
        if (role == null) {
            throw new AppException("Provider Role not set.");
        }

        User provider = new User(
                null,
                "provider",
                passwordEncoder.encode("asdfk2.daADd"),
                "provider@mail.com",
                role,
                false,
                false,
                null,
                null
        );

        String name = "Mr";
        String surname = "Provider";
        Profile profile = new Profile(
                provider,
                name,
                surname,
                new Date(),
                "https://ui-avatars.com/api/?name=" + name + "+" + surname + "&rounded=true&%20bold=true&" +
                        "background=a8d267&color=000000"
        );
        provider.setProfile(profile);

        // Create wallet
        Wallet wallet = new Wallet(provider, 0.00);
        provider.setWallet(wallet);

        Business business = createTestBusiness(provider);

        // Assign the business to the provider
        provider.setBusiness(business);

        userRepository.save(provider);
    }

    // Not a SpringTest. This is called by the "insertProvider()"-Test.
    private Business createTestBusiness(User provider) {

        // Create business.
        Business business = new Business(
                "Business_name",
                "info@business_name.com",
                "address",
                "tax_number",
                "tax_office",
                "owner_name",
                "owner_surname",
                "owner_patronym",
                "id_card_number",
                new Date(),
                "residence_address",
                provider,
                new Wallet(provider, 0.00)
        );

        // Create 2 hotels each having 3 rooms.
        int numOfHotels = 5;      //    must be <= 5
        int numOfRooms = 3;
        int numOfCalendarsEntriesPerRoom = 2;

        List<Hotel> hotels = new ArrayList<>();
        Hotel hotel;

        List<Room> rooms;
        Room room;


        //////////////            athens - rome - london - paris  /////////////////////////////
        double countrieslat[] = {
                37.9838, 41.9028, 51.5074, 48.8566
        };

        double countrieslng[] = {
                23.7275, 12.4964, -0.127758, 2.3522
        };

        List<String> names = Arrays.asList(
                "Parrotel Beach Resort Ex. Radisson Blu",
                "Baron Resort Sharm El Sheikh",
                "Xperience St. George Sharm El Sheikh",
                "Solymar Naama Bay",
                "Marina Sharm Hotel (Ex. Helnan Marina)"

                ,

                "Hotel Capannelle Roma ",
                "Quality Hotel Rouge et Noir",
                "Hotel Roma Aurelia Antica",
                "Hotel Piccolo Borgo",
                "Bellambriana"

                ,

                "Britannia International Hotel Canary Wharf",
                "Novotel London Canary Wharf",
                "Hampstead Britannia",
                "DoubleTree by Hilton London",
                "Central Park Hotel"

                ,

                "Amarante Beau Manoir",
                "Oh la la ! Hotel Bar Paris Bastille",
                "La Maison Armance",
                "Le Temple De Jeanne",
                "Hôtel Arvor Saint Georges"


        );

        List<String> descriptionLongs = Arrays.asList(
                "Overlooking a private beach and the turquoise waters of Naama Bay, Marina Sharm Hotel offers spacious rooms, most of them with private balconies. There are 2 outdoor pools and a wellness centre. WiFi access to be available at the lobby area and it’s free of charge\n" +
                        "\n" +
                        "Rooms at this 4-star hotel all have floor-to-ceiling windows and modern furnishings. They come equipped with air conditioning, minibar and satellite flat-screen TV. All rooms offer Red Sea views.\n" +
                        "\n" +
                        "Dining options at the Marina Sharm include the Mermaid seafood restaurant, also serving Italian cuisine. The beach bar offers cocktails, refreshing beverages and live music entertainment.\n" +
                        "\n" +
                        "Guests of the Marina Sharm can treat themselves to a rejuvenating massage, or relax in the spa pool, hammam or sauna. The hotel also has a children’s playground, and a games room with billiards.\n" +
                        "\n" +
                        "The Marina Sharm is 11 km from Sharm El Sheikh International Airport, 7 km from the Old Market and a 15-minute drive from the town’s centre. The hotel provides 24-hour front desk service.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Σαρμ Ελ Σέιχ σύμφωνα με ανεξάρτητα σχόλια.\n" +
                        "\n" +
                        "Το κατάλυμα αυτό βρίσκεται επίσης σε μία από τις τοποθεσίες με την καλύτερη βαθμολογία στο Σαρμ Ελ Σέιχ! Αρέσει περισσότερο στους επισκέπτες σε σχέση με άλλα καταλύματα στην περιοχή.\n" +
                        "\n" +
                        "Η τοποθεσία αρέσει ιδιαίτερα σε ζευγάρια – τη βαθμολόγησαν με 8,8 για ταξίδι δύο ατόμων.",
                "Αυτό το πολυτελές θέρετρο διαθέτει ιδιωτική παραλία, 3 πισίνες, 3 γήπεδα τένις και γήπεδο βόλεϊ. Προσφέρει επίσης 5 εστιατόρια, καθημερινή ψυχαγωγία και θαλάσσια αθλήματα.\n" +
                        "\n" +
                        "Το 5 αστέρων Parrotel Beach Resort παρέχει δωμάτια διακοσμημένα με πλούσια υφάσματα. Όλα έχουν ιδιωτικό μπαλκόνι και δορυφορική τηλεόραση. Στο λόμπι και στα Marine Club δωμάτια παρέχεται δωρεάν WiFi.\n" +
                        "\n" +
                        "Το πλήρως εξοπλισμένο σπα του θερέτρου προσφέρει σάουνες, υδρομασάζ και περιποιήσεις μασάζ από έμπειρο προσωπικό. Οι επισκέπτες μπορούν επίσης να κάνουν μια βουτιά στη μεγάλη εξωτερική πισίνα του θερέτρου, η οποία περιβάλλεται από φοίνικες. Για τους μικρούς επισκέπτες, το θέρετρο διαθέτει το Abracadabra Club που προσφέρει ψυχαγωγία υπό την επίβλεψη προσωπικού.\n" +
                        "\n" +
                        "Τα πολλά εστιατόρια του θερέτρου σερβίρουν ποικιλία εκλεκτών εδεσμάτων, όπως παραδοσιακά αιγυπτιακά πιάτα, υψηλής ποιότητας ιταλικές σπεσιαλιτέ, εξαιρετικά θαλασσινά, καθώς και πίτσα ψημένη σε ξυλόφουρνο. Στα πλήρους εξυπηρέτησης μπαρ και καφέ του θερέτρου μπορείτε να απολαύσετε κοκτέιλ και καφέ.\n" +
                        "\n" +
                        "Το κατάλυμα απέχει 8χλμ. από το Διεθνές Αεροδρόμιο του Σαρμ Ελ Σέιχ και την Πλατεία Soho και 17χλμ. από τον Κόλπο Naama. Καθημερινά οργανώνεται υπηρεσία μεταφοράς κατόπιν αιτήματος.\n" +
                        "\n" +
                        "Το κατάλυμα αυτό βρίσκεται επίσης σε μία από τις τοποθεσίες με την καλύτερη βαθμολογία στο Σαρμ Ελ Σέιχ! Αρέσει περισσότερο στους επισκέπτες σε σχέση με άλλα καταλύματα στην περιοχή.\n" +
                        "\n" +
                        "Η τοποθεσία αρέσει ιδιαίτερα σε ζευγάρια – τη βαθμολόγησαν με 8,7 για ταξίδι δύο ατόμων.\n" +
                        "\n" +
                        "Αυτό το κατάλυμα έχει αξιολογηθεί ως αυτό με την καλύτερη σχέση ποιότητας τιμής στο Σαρμ Ελ Σέιχ! Εδώ τα χρήματα των επισκεπτών έχουν μεγαλύτερη αξία σε σχέση με άλλα καταλύματα στην πόλη.",
                "Αυτό το κομψό, στιλάτο, αριστοκρατικό θέρετρο 5 αστέρων βρίσκεται στο Sharm El Sheikh και απέχει 600μ. από την πανέμορφη, ιδιωτική, μεταξένια, λευκή, αμμώδη παραλία. Διαθέτει πισίνα σε στιλ λιμνοθάλασσας και μια πλατφόρμα μήκους 145μ. για κολύμβηση με αναπνευστήρα και καταδύσεις. Έχει 9 εστιατόρια και μπαρ. Περιλαμβάνει επίσης μεγάλη αμμώδη παραλία, ολυμπιακών διαστάσεων πισίνα με γλυκό νερό (θερμαινόμενη το χειμώνα) και μόνο για ενήλικες, υδρομασάζ με πίδακες με εν μέρει θαλασσινό νερό, καθώς και παιδική πισίνα. Οι επισκέπτες μπορούν να επωφεληθούν από τη δωρεάν υπηρεσία μεταφοράς με λεωφορείο προς το συγκρότημα Soho Square μία φορά την ημέρα, το βράδυ.\n" +
                        "\n" +
                        "Τα δωμάτια του Baron Resort παρέχουν επιλογή μαξιλαριού, δορυφορική τηλεόραση και κλινοσκεπάσματα από 100% αιγυπτιακό βαμβάκι. Ακόμη, διαθέτουν παράθυρα από το δάπεδο μέχρι την οροφή και επιπλωμένο μπαλκόνι. Ορισμένα έχουν θέα στην Ερυθρά Θάλασσα, το νησί Tiran ή τους κήπους του θερέτρου.\n" +
                        "\n" +
                        "Το Baron Resort προσφέρει 9 επιλογές για φαγητό και ποτό, μεταξύ αυτών το παραλιακό Taj Mahal που σερβίρει ινδικές σπεσιαλιτέ. Στο μπαρ Now & Zen Dance θα απολαύσετε εξωτικά κοκτέιλ και μουσική κατά τις βραδινές ώρες. Επίσης, διοργανώνονται διάφορες θεματικές βραδιές στα εστιατόρια, όπως παραδοσιακά δείπνα Βεδουίνων. Στο Al Sakia Seafood Restaurant και το Oasis Pool Restaurant & Bar μπορείτε να απολαύσετε ζωντανό ψυχαγωγικό πρόγραμμα.\n" +
                        "\n" +
                        "Στο κέντρο ευεξίας και χαλάρωσης του Baron Resort διατίθενται περιτυλίξεις σώματος με φύκια και αιγυπτιακά μασάζ. Επίσης, υπάρχουν γυμναστήριο και ατμόλουτρα. Στο θέρετρο έχετε τη δυνατότητα να επιδοθείτε και σε άλλες δραστηριότητες, όπως beach volley, τρέξιμο και ποδηλασία σε πίστα. Για τους μικρούς επισκέπτες, υπάρχει παιδική λέσχη με χώρο ύπνου και προσωπικό επιτήρησης.\n" +
                        "\n" +
                        "Το 24ωρο προσωπικό μπορεί να οργανώσει εκδρομές στο εθνικό πάρκο Ras Mohammed, το οποίο απέχει 1 ώρα και 30 λεπτά με το σκάφος. Το Baron Resort Sharm El Sheikh βρίσκεται σε απόσταση 3χλμ. από τις επιλογές διασκέδασης του Soho Square και 15χλμ. από την περιοχή Naama Bay. Το Διεθνές Αεροδρόμιο Sharm El Sheikh είναι προσβάσιμο σε 8χλμ. από το κατάλυμα.\n" +
                        "\n" +
                        "Η τοποθεσία αρέσει ιδιαίτερα σε ζευγάρια – τη βαθμολόγησαν με 8,1 για ταξίδι δύο ατόμων.\n" +
                        "\n" +
                        "Αυτό το κατάλυμα έχει αξιολογηθεί ως αυτό με την καλύτερη σχέση ποιότητας τιμής στο Σαρμ Ελ Σέιχ! Εδώ τα χρήματα των επισκεπτών έχουν μεγαλύτερη αξία σε σχέση με άλλα καταλύματα στην πόλη.",
                "Αυτό το θέρετρο 4 αστέρων βρίσκεται σε απόσταση 2χλμ. από την Ερυθρά Θάλασσα και διαθέτει ιδιωτική αμμώδη παραλία. Το κατάλυμα έχει πρόσβαση σε 2 παραλίες, η μία με αμμουδιά και η άλλη με κοράλλια στο βυθό για κολύμβηση με αναπνευστήρα και καταδύσεις. Θα βρείτε ακόμη 4 εξωτερικές πισίνες, ηλιόλουστη βεράντα και μια ιδιωτική παραλία με χρυσή αμμουδιά, όπου λειτουργεί παραθαλάσσιο εστιατόριο-beach bar. Το Εθνικό Πάρκο Ras Mohammad είναι 30χλμ. μακριά.\n" +
                        "\n" +
                        "Το Xperience St. George προσφέρει κλιματιζόμενα δωμάτια με ιδιωτικό μπάνιο. Όλα τα δωμάτια περιλαμβάνουν δορυφορική τηλεόραση και επιφάνεια εργασίας.\n" +
                        "\n" +
                        "Οι επισκέπτες μπορούν να απολαύσουν το πρωινό τους στο εστιατόριο του ξενοδοχείου ή στο δωμάτιό τους. Το Xperience φιλοξενεί ένα εστιατόριο à la carte και ένα εστιατόριο με μπουφέ, ενώ ετοιμάζει και γεύματα σε πακέτο. Προσφέρονται επίσης μενού ειδικής διατροφής, κατόπιν αιτήματος.\n" +
                        "\n" +
                        "Το γυμναστήριο και το σπα του ξενοδοχείου, το οποίο περιλαμβάνει σάουνα και υδρομασάζ, είναι ιδανικά για χαλάρωση. Το St. George Homestay διαθέτει επίσης εγκαταστάσεις θαλάσσιων σπορ, πινγκ πονγκ, μπιλιάρδου και βόλεϊ.\n" +
                        "\n" +
                        "Το Διεθνές Αεροδρόμιο του Sharm el-Sheikh απέχει μόλις 18 χιλιόμετρα. Το ξενοδοχείο μπορεί να οργανώσει υπηρεσία μεταφοράς και παρέχει δωρεάν ιδιωτικό χώρο στάθμευσης στις εγκαταστάσεις του. Ο Κόλπος Naama, προς τον οποίο προσφέρεται δωρεάν υπηρεσία μεταφοράς, είναι 5χλμ. μακριά.\n" +
                        "\n" +
                        "Αυτό το κατάλυμα έχει αξιολογηθεί ως αυτό με την καλύτερη σχέση ποιότητας τιμής στο Σαρμ Ελ Σέιχ! Εδώ τα χρήματα των επισκεπτών έχουν μεγαλύτερη αξία σε σχέση με άλλα καταλύματα στην πόλη.",
                "Το Solymar Naama Bay βρίσκεται σε ιδανική τοποθεσία, στο κέντρο του δημοφιλούς Κόλπου Naama, σε απόσταση μόλις 10 λεπτών από το Σαρμ. Το κατάλυμα προσφέρει WiFi κατόπιν αιτήματος, εξωτερική πισίνα και πινγκ-πονγκ. Το Διεθνές Αεροδρόμιο Sharm El Sheikh είναι προσβάσιμο σε 10 λεπτά οδικώς.\n" +
                        "\n" +
                        "Όλα τα δωμάτια του Solymar Naama Bay διαθέτουν μπαλκόνι, τηλεόραση επίπεδης οθόνης και μίνι μπαρ. Το μπάνιο περιλαμβάνει ντους και μπανιέρα.\n" +
                        "\n" +
                        "Μπορείτε να απολαύσετε διάφορα πιάτα στο κεντρικό εστιατόριο, που έχει μεγάλη βεράντα με θέα στον Κόλπο Naama και την Ερυθρά Θάλασσα. Τα 2 μπαρ του θέρετρου σερβίρουν διάφορα ποτά. Στις εγκαταστάσεις του καταλύματος προσφέρονται διάφορες δραστηριότητες, όπως κολύμβηση με αναπνευστήρα, καταδύσεις και ιστιοσανίδα.\n" +
                        "\n" +
                        "Το Εθνικό Πάρκο Ras Mohammed απέχει 16χλμ. από το κατάλυμα. Παρέχεται δωρεάν υπηρεσία μεταφοράς με λεωφορείο προς την παραλία. Μπορείτε να κάνετε καταδύσεις, κανό και ιππασία κοντά στο ξενοδοχείο, με πρόσθετη χρέωση.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Σαρμ Ελ Σέιχ σύμφωνα με ανεξάρτητα σχόλια.",
                "Το Hotel Capannelle προσφέρει καλοκαιρινή πισίνα και απέχει 6χλμ. από το Αεροδρόμιο Ciampino της Ρώμης. Διαθέτει εστιατόριο και κλιματιζόμενα δωμάτια με δωρεάν WiFi.\n" +
                        "\n" +
                        "Τα δωμάτια στο ξενοδοχείο Capannelle παρέχουν μοντέρνα διακόσμηση, τηλεόραση επίπεδης οθόνης με pay per view κανάλια και μίνι μπαρ. Ορισμένα έχουν θέα στον κήπο, ενώ σε άλλα θα βρείτε βεράντα.\n" +
                        "\n" +
                        "Το εστιατόριο Marco Polo του ξενοδοχείου προσφέρει ρωμαϊκές σπεσιαλιτέ και κλασικά ιταλικά πιάτα. Στο lounge υπάρχει μπαρ, στο οποίο μπορείτε να απολαύσετε ποτά και σνακ. Καθημερινά σερβίρεται μπουφές πρωινού.",
                "Το Quality Hotel Rouge διαθέτει δωρεάν Wi-Fi υψηλής ταχύτητας, κλιματιζόμενα δωμάτια, βεράντα και εξωτερική πισίνα την οποία μπορείτε να χρησιμοποιήσετε δωρεάν. Παρέχεται δωρεάν υπηρεσία μεταφοράς με προγραμματισμένα δρομολόγια από/προς τον Σιδηροδρομικό Σταθμό Tiburtina.\n" +
                        "\n" +
                        "Όλα τα δωμάτια περιλαμβάνουν smartphone για δωρεάν κλήσεις και πληροφορίες για την πόλη, δορυφορική τηλεόραση και μίνι μπαρ. Τα περισσότερα έχουν μπαλκόνι με θέα στην πόλη.\n" +
                        "\n" +
                        "Καθημερινά, σερβίρεται πλούσιο πρωινό με γλυκά και αλμυρά εδέσματα, όπως ομελέτες, μπέικον και φρεσκοψημένα κέικ από τον σεφ ζαχαροπλαστικής του ξενοδοχείου. Προσφέρονται επίσης επιλογές χωρίς γλουτένη.\n" +
                        "\n" +
                        "Το κέντρο της Ρώμης απέχει 7χλμ. από το κατάλυμα. Ο Σιδηροδρομικός Σταθμός Tiburtina της Ρώμης είναι προσβάσιμος σε 10 λεπτά με το αυτοκίνητο.\n" +
                        "\n" +
                        "Η περιοχή Tiburtino είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: σιντριβάνια, μνημεία και αρχαία μνημεία.",
                "Το Hotel Roma Aurelia Antica προσφέρει εποχική εξωτερική πισίνα, à la carte εστιατόριο και ηλιόλουστη βεράντα. Το κατάλυμα βρίσκεται μέσα σε ένα ιδιωτικό πάρκο και παρέχει δωρεάν χώρο στάθμευσης στις εγκαταστάσεις.\n" +
                        "\n" +
                        "Όλα τα κλιματιζόμενα δωμάτια στο Roma Aurelia Antica περιλαμβάνουν μίνι μπαρ και τηλεόραση με δορυφορικά και pay per view κανάλια. Σε ορισμένα δωμάτια υπάρχει επίσης συσκευή για τσάι/καφέ.\n" +
                        "\n" +
                        "Το εστιατόριο έχει χώρο φαγητού δίπλα στην πισίνα και ρομαντική βεράντα, ενώ σερβίρει κλασική ιταλική κουζίνα και ισπανικές σπεσιαλιτέ. Διατίθεται υπηρεσία κέτερινγκ για δεξιώσεις και άλλες ιδιωτικές εκδηλώσεις.\n" +
                        "\n" +
                        "Δίπλα στο ξενοδοχείο λειτουργεί ένα πλήρως εξοπλισμένο αθλητικό κέντρο με γυμναστήριο, πισίνα, σάουνα και γήπεδα τένις και ποδοσφαίρου.\n" +
                        "\n" +
                        "Παρέχεται τακτική υπηρεσία μεταφοράς από/προς τον σταθμό μετρό Valle Aurelia και το Διεθνές Αεροδρόμιο Fiumicino. Απαιτείται κράτηση εκ των προτέρων.\n" +
                        "\n" +
                        "Η περιοχή Aurelio είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: σιντριβάνια, εξερεύνηση παλιάς πόλης και μουσεία.",
                "Το Hotel Piccolo Borgo απέχει 500μ. από το σιδηροδρομικό σταθμό Capannelle. Βρίσκεται δίπλα στο Περιφερειακό Πάρκο Appian Way, στα προάστια της Ρώμης και κάποτε ήταν αγροικία. Προσφέρεται δωρεάν χώρος στάθμευσης.\n" +
                        "\n" +
                        "Τα δωμάτια διαθέτουν ιδιωτική είσοδο και κλασική ή μοντέρνα διακόσμηση. Παρέχουν παρκέ δάπεδα ή πατώματα με κεραμικά πλακάκια. Περιλαμβάνουν τηλεόραση επίπεδης οθόνης με δορυφορικά και pay per view κανάλια.\n" +
                        "\n" +
                        "Το εστιατόριο είναι ανοιχτό για δείπνο 6 ημέρες την εβδομάδα. Καθημερινά σερβίρεται πρωινό με γλυκά εδέσματα.\n" +
                        "\n" +
                        "Οι επισκέπτες μπορούν να χαλαρώσουν στον κήπο, στην ηλιόλουστη βεράντα ή στην εξωτερική πισίνα. Το Hotel Piccolo Borgo διαθέτει 24ωρη ρεσεψιόν.\n" +
                        "\n" +
                        "Το ξενοδοχείο προσφέρει δωρεάν υπηρεσία μεταφοράς με προγραμματισμένα δρομολόγια από/προς το σιδηροδρομικό σταθμό Capannelle και το σταθμό Cinecittà του μετρό.",
                "Το Bellambriana βρίσκεται αρκετά μακριά από το πολύβουο κέντρο, για να απολαύσετε την ηρεμία και τη γαλήνη, αλλά αρκετά κοντά για να εξερευνήσετε την Αιώνια Πόλη και διαθέτει δική του πισίνα.\n" +
                        "\n" +
                        "Το Bellambriana περιλαμβάνει εξωτερική πισίνα, όπου μπορείτε να δροσιστείτε ή να χαλαρώσετε στη βεράντα με τις ξαπλώστρες και τις ομπρέλες. Έχετε τη δυνατότητα να απολαύσετε δροσιστικό ποτό στο μπαρ της πισίνας. Το ξενοδοχείο προσφέρει επίσης πλήρως εξοπλισμένες αίθουσες συνεδριάσεων με μέγιστη χωρητικότητα 150 ατόμων.\n" +
                        "\n" +
                        "Τα δωμάτια στο Bellambriana διαθέτουν δορυφορική τηλεόραση, internet και κλιματισμό. Ξεκινήστε τη μέρα σας με μπουφέ πρωινού, πριν εξερευνήσετε τη Ρώμη.\n" +
                        "\n" +
                        "Το λεωφορείο, το οποίο κάνει στάση ακριβώς έξω από το ξενοδοχείο, παρέχει συνδέσεις με το σταθμό του μετρό μέσα σε λίγα μόλις λεπτά. Από εκεί μπορείτε να φτάσετε εύκολα στο ιστορικό κέντρο και το Βατικανό.\n" +
                        "\n" +
                        "Το βράδυ, απολαύστε νόστιμη μεσογειακή κουζίνα και παραδοσιακή ναπολιτάνικη πίτσα στο εστιατόριο.",
                " Ένα από τα καταλύματά μας με τις υψηλότερες πωλήσεις στο Λονδίνο!\n" +
                        "Το Britannia International Hotel Canary Wharf βρίσκεται σε απόσταση μόλις 600μ. από το σταθμό Canary Wharf του μετρό και διαθέτει ευρύχωρα δωμάτια με περιορισμένο WiFi στα υπνοδωμάτια.\n" +
                        "\n" +
                        "Τα παραδοσιακά δωμάτια στο Britannia International Canary Wharf περιλαμβάνουν ιδιωτικό μπάνιο, ορισμένα με μπανιέρα-υδρομασάζ. Όλα τα δωμάτια προσφέρουν παροχές για τσάι/καφέ και επιφάνεια εργασίας. Πολλά έχουν θέα στην περιοχή Docklands.\n" +
                        "\n" +
                        "Το Jenny’s Restaurant σερβίρει παραδοσιακή βρετανική κουζίνα και μαγειρευτό πρωινό. Το Pizzeria προτείνει ιταλική κουζίνα, ελαφριά μεσημεριανά γεύματα και σνακ.\n" +
                        "\n" +
                        "Το Hotel Britannia International Canary Wharf απέχει 10 λεπτά οδικώς από το στάδιο O2 Arena και 15 λεπτά με τα πόδια από την περιοχή Millwall. Το Γκρίνουιτς είναι προσβάσιμο σε 15 λεπτά οδικώς. Στον χώρο του καταλύματος λειτουργεί χώρος στάθμευσης.",
                "Το Novotel London Canary Wharf προσφέρει γυμναστήριο, πισίνα, σύγχρονο εστιατόριο και μπαρ, καθώς επίσης βεράντα στον τελευταίο όροφο με πανοραμική θέα στον ορίζοντα του Λονδίνου. Σε όλους τους χώρους του ξενοδοχείου υπάρχει δωρεάν WiFi.\n" +
                        "\n" +
                        "Όλα τα κλιματιζόμενα δωμάτια περιλαμβάνουν τηλεόραση επίπεδης οθόνης, ιδιωτικό μπάνιο και επιφάνεια εργασίας.\n" +
                        "\n" +
                        "Η ρεσεψιόν του καταλύματος λειτουργεί όλο το 24ωρο. Διατίθεται υπηρεσία δωματίου με μια ποικιλία από ζεστά πιάτα και κρύα σνακ. Το Bōkan Restaurant, Bar and Roof Terrace σερβίρει περίτεχνα φαγητά και ποτά, που παρασκευάζονται με τοπικά, παραδοσιακά υλικά από τη Βρετανία. Το μενού είναι εμπνευσμένο από την ευρωπαϊκή κουζίνα. Προσφέρονται πανοραμική θέα στο Λονδίνο και τον Τάμεση και WiFi υψηλής ταχύτητας. Μπορείτε να γευματίσετε στους 3 τελευταίους ορόφους.\n" +
                        "\n" +
                        "Το Novotel London Canary Wharf απέχει 3,4χλμ. από το Πάρκο του Γκρίνουιτς και 3,6χλμ. από τη Γέφυρα του Λονδίνου. Το πλησιέστερο αεροδρόμιο είναι το αεροδρόμιο City του Λονδίνου, σε απόσταση 5χλμ. από το Novotel London Canary Wharf.\n" +
                        "\n" +
                        "Η περιοχή Tower Hamlets είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: ατμόσφαιρα, βολικές συγκοινωνίες και μνημεία.",
                " Ένα από τα καταλύματά μας με τις υψηλότερες πωλήσεις στο Λονδίνο!\n" +
                        "Το Hampstead Britannia απέχει 5 λεπτά με τα πόδια από τον συναυλιακό χώρο Roundhouse και το σταθμό Chalk Farm του μετρό. Προσφέρει εστιατόριο με σέρα και μπαρ με εξωτερική βεράντα.\n" +
                        "\n" +
                        "Τα δωμάτια στο Britannia Hampstead διαθέτουν τηλεόραση, πρέσα παντελονιών και δωρεάν εφημερίδα. Περιλαμβάνουν επίσης παροχές για τσάι και καφέ. Επιπλέον, σε όλους τους χώρους διατίθεται δωρεάν Wi-Fi.\n" +
                        "\n" +
                        "Το ευρύχωρο εστιατόριο σερβίρει διεθνή κουζίνα, καθώς επίσης κρύο και ζεστό μπουφέ πρωινού. Το μπαρ προσφέρει μενού με σνακ και μεγάλη ποικιλία ποτών.\n" +
                        "\n" +
                        "Το Primrose Hill και ο ζωολογικός κήπος του Λονδίνου απέχουν 10 λεπτά με τα πόδια από το ξενοδοχείο Hampstead Britannia, ενώ η αγορά του Camden και το Regents Park βρίσκονται λιγότερο από 1,6χλμ. μακριά.\n" +
                        "\n" +
                        "Η περιοχή Κάμντεν είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: θέατρο, ψώνια και διασκέδαση.",
                "Το DoubleTree by Hilton London - Docklands Riverside προσφέρει κατάλυμα 4 αστέρων δίπλα στον ποταμό Τάμεση, με εκπληκτική θέα στο συγκρότημα κτηρίων Canary Wharf. Παρέχει δωρεάν Wi-Fi και δωρεάν πρόσβαση στο γυμναστήριο σε όλους τους επισκέπτες.\n" +
                        "\n" +
                        "Αυτό το DoubleTree by Hilton στεγάζεται σε πρώην κτήριο της αποβάθρας του 17ου αιώνα, που αποτελούσε τον τελευταίο ναύσταθμο του Λονδίνου για τη ναυπήγηση πλοίων και είναι διακοσμημένο με εκτεθειμένα τούβλα. Όλα τα δωμάτια είναι πλήρως εξοπλισμένα με ιδιωτικό μπάνιο, τηλεόραση επίπεδης οθόνης, κλιματισμό, ανοιγόμενα παράθυρα και παροχές για τσάι/καφέ. Οι περισσότερες μονάδες έχουν θέα στην πόλη ή στον ποταμό. Στις εγκαταστάσεις λειτουργούν 24ωρη υπηρεσία δωματίου και 24ωρη ρεσεψιόν.\n" +
                        "\n" +
                        "Το Columbia Restaurant σερβίρει εποχικές λιχουδιές και διοργανώνει ιδιωτικά μπάρμπεκιου στην εξωτερική βεράντα με θέα στο συγκρότημα κτηρίων Canary Wharf και τον ποταμό Τάμεση. Για τον εορτασμό ειδικών περιστάσεων, θα βρείτε τα εστιατόρια The Library και Snug, όπου θα απολαύσετε ιδιωτικά δείπνα και εξαιρετική εξυπηρέτηση.\n" +
                        "\n" +
                        "Το παραποτάμιο κατάλυμα προσφέρει ιδιωτικό φέρι που σας μεταφέρει δωρεάν στην προβλήτα του Canary Wharf. Διατίθεται χώρος στάθμευσης με επιπλέον χρέωση.",
                "Το Central Park Hotel απέχει λιγότερο από 100μ. από το Hyde Park και μόλις 2 λεπτά με τα πόδια από τον σταθμό Queensway του υπόγειου σιδηρόδρομου. Tα καταστήματα, τα μπαρ και τα εστιατόρια της περιοχής Κένσινγκτον είναι προσβάσιμα σε 15 λεπτά με τα πόδια. Η ρεσεψιόν του Central Park Hotel λειτουργεί όλο το 24ωρο.\n" +
                        "\n" +
                        "Όλα τα απλά δωμάτια διαθέτουν δορυφορική τηλεόραση, στεγνωτήρα μαλλιών και παροχές για τσάι και καφέ. Επίσης, περιλαμβάνουν ιδιωτικό μπάνιο με επιλεγμένα προϊόντα περιποίησης.\n" +
                        "\n" +
                        "Στο ευρύχωρο lounge σερβίρονται ελαφριά σνακ και ποικιλία ποτών. Υπάρχει επίσης ένα σύγχρονο cocktail bar. Καθημερινά προσφέρεται πλήρες πρωινό.\n" +
                        "\n" +
                        "Τα καταστήματα της Oxford Street απέχουν 5 λεπτά με το μετρό από τον σταθμό Queensway, ενώ για τα ανάκτορα και τους κήπους του Μπάκιγχαμ η διαδρομή διαρκεί 20 λεπτά. Ο σιδηροδρομικός σταθμός Paddington, που εξυπηρετεί τα τρένα Heathrow Express, βρίσκεται σε απόσταση 15 λεπτών με τα πόδια.\n" +
                        "\n" +
                        "Η περιοχή Γουεστμίνστερ είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: αγορές-μάρκες πολυτελείας, αγορές για ρούχα και θέατρο.\n" +
                        "\n" +
                        "Η τοποθεσία αρέσει ιδιαίτερα σε ζευγάρια – τη βαθμολόγησαν με  0,3 / 10 για ταξίδι δύο ατόμων.",
                "Το Amarante Beau απέχει μόλις 5 λεπτά με τα πόδια από την Όπερα Garnier και τα Ηλύσια Πεδία στο Παρίσι. Προσφέρει δωμάτια και σουίτες 4 αστέρων.\n" +
                        "\n" +
                        "Κάθε δωμάτιο στο Amarante είναι ηχομονωμένο και διαθέτει δορυφορική τηλεόραση και κλιματισμό. Περιλαμβάνουν επίσης ιδιωτικό μπάνιο με μαρμάρινα έπιπλα.\n" +
                        "\n" +
                        "Καθημερινά σερβίρεται πρωινό στην τραπεζαρία του ξενοδοχείου στο κελάρι. Οι επισκέπτες μπορούν να χαλαρώσουν στο μπαρ του Amarante Beau Manoir.\n" +
                        "\n" +
                        "Το Beau Manoir απέχει μόλις 2 λεπτά με τα πόδια από το σταθμό Madeleine του μετρό, ο οποίος παρέχει απευθείας συνδέσεις με την περιοχή της Μονμάρτης και τον Πύργο του Άιφελ.\n" +
                        "\n" +
                        "Η περιοχή 8ο διαμ. είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: ψώνια, ρομαντική ατμόσφαιρα και αξιοθέατα.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Παρίσι σύμφωνα με ανεξάρτητα σχόλια. Αυτή η περιοχή είναι ιδανική για αγορές, με πολλά επώνυμα καταστήματα κοντά: Gucci, Hermès, Ralph Lauren, Chanel, Burberry."
                ,
                "Guests at Oh la la! Hotel Bar Paris Bastille can enjoy a buffet breakfast.\n" +
                        "\n" +
                        "The accommodation can conveniently provide information at the reception to help guests to get around the area.\n" +
                        "\n" +
                        "The nearest airport is Paris - Orly Airport, 16 km from Oh la la! Hotel Bar Paris Bastille.\n" +
                        "\n" +
                        "Η περιοχή 11ο διαμ. είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: μνημεία, πολιτισμό και μουσεία.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Παρίσι σύμφωνα με ανεξάρτητα σχόλια.",
                "Μείνετε στην καρδιά του προορισμού Παρίσι – Υπέροχη τοποθεσία - εμφάνιση χάρτη\n" +
                        "Το Maison Armance βρίσκεται στο κέντρο του Παρισιού, σε απόσταση 50μ. από τον Κήπο του Κεραμεικού και 200μ. από την Πλατεία Κονκόρντ. Το κατάλυμα ήταν κάποτε κατοικία του Γάλλου συγγραφέα Σταντάλ και προσφέρει δωρεάν WiFi σε όλους τους χώρους.\n" +
                        "\n" +
                        "Όλα τα δωμάτια έχουν ξεχωριστή διακόσμηση και διαθέτουν δορυφορική τηλεόραση, μίνι μπαρ και ιδιωτικό μπάνιο.\n" +
                        "\n" +
                        "Καθημερινά σερβίρεται πρωινό στην τραπεζαρία ή στο δωμάτιό σας, κατόπιν αιτήματος. Το Maison Armance εξυπηρετείται από ανελκυστήρα. Η ρεσεψιόν λειτουργεί όλο το 24ωρο.\n" +
                        "\n" +
                        "Το ξενοδοχείο απέχει 10 λεπτά με τα πόδια από το Μουσείο του Λούβρου και 500μ. από τη Λεωφόρο των Ηλυσίων Πεδίων. Σε απόσταση 180μ. βρίσκεται ο σταθμός Concorde του μετρό, το οποίο παρέχει άμεση πρόσβαση σε πολλά αξιοθέατα του Παρισιού.\n" +
                        "\n" +
                        "Η περιοχή 1ο διαμ. είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: φαγητό, ψώνια και μουσεία.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Παρίσι σύμφωνα με ανεξάρτητα σχόλια. Αυτή η περιοχή είναι ιδανική για αγορές, με πολλά επώνυμα καταστήματα κοντά: Rolex, Cartier, Chanel, Louis Vuitton, H&M.\n" +
                        "\n" +
                        "Το κατάλυμα αυτό βρίσκεται επίσης σε μία από τις τοποθεσίες με την καλύτερη βαθμολογία στο Παρίσι! Αρέσει περισσότερο στους επισκέπτες σε σχέση με άλλα καταλύματα στην περιοχή."
                ,
                "Μείνετε στην καρδιά του προορισμού Παρίσι – Υπέροχη τοποθεσία - εμφάνιση χάρτη\n" +
                        "Σύντομα η παρακάτω περιγραφή καταλύματος θα είναι διαθέσιμη και στη γλώσσα σας. Λυπούμαστε για την ταλαιπωρία.\n" +
                        "Le Temple De Jeanne is a design hotel located in the heart of Paris. It offers individually-decorated rooms and suites equipped with free Wi-Fi access.\n" +
                        "\n" +
                        "Each room at Le Temple De Jeanne has a flat-screen TV and a private bathroom.\n" +
                        "\n" +
                        "An organic buffet breakfast is served daily in the breakfast room. The hotel also provides a 24-hour reception.\n" +
                        "\n" +
                        "The Pointe Rivoli Hotel is less than a 2-minute walk from Saint-Paul Metro station, which provides direct access to Le Louvre and the Champs-Elysees. The River Seine is 300 metres away. Notre Dame Cathedral is a 10-minute walk away.\n" +
                        "\n" +
                        "Η περιοχή 4ο διαμ. είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: φαγητό, ψώνια και τέχνη.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Παρίσι σύμφωνα με ανεξάρτητα σχόλια. Αυτή η περιοχή είναι ιδανική για αγορές, με πολλά επώνυμα καταστήματα κοντά: H&M, Zara, Chanel.\n" +
                        "\n" +
                        "Το κατάλυμα αυτό βρίσκεται επίσης σε μία από τις τοποθεσίες με την καλύτερη βαθμολογία στο Παρίσι! Αρέσει περισσότερο στους επισκέπτες σε σχέση με άλλα καταλύματα στην περιοχή."
                ,
                "ο Arvor βρίσκεται σε ιδανική τοποθεσία, σε έναν ήσυχο δρόμο στην καρδιά του Παρισιού, και προσφέρει δωρεάν Wi-Fi, καθαρά, άνετα δωμάτια με σύγχρονες εγκαταστάσεις και μια χαλαρή, φιλική ατμόσφαιρα.\n" +
                        "\n" +
                        "Τα ζεστά δωμάτια είναι εξοπλισμένα με όλες τις σύγχρονες ανέσεις, όπως δορυφορική τηλεόραση και ιδιωτικό μπάνιο. Τα περισσότερα έχουν θέα πάνω από τις στέγες του Παρισιού, ενώ από ορισμένα μπορείτε να δείτε τον Πύργο του Άιφελ.\n" +
                        "\n" +
                        "Το εξυπηρετικό προσωπικό σε αυτό το υπέροχο ξενοδοχείο θα σας βοηθήσει να αξιοποιήσετε στο έπακρο την επίσκεψή σας. Το πρωινό σερβίρεται καθημερινά και ένας υπολογιστής είναι διαθέσιμος στο λόμπι για τη χρήση των φιλοξενουμένων.\n" +
                        "\n" +
                        "Εξερευνήστε το Παρίσι με τα πόδια ή με τις εξαίρετες συγκοινωνιακές συνδέσεις. Στους επισκέπτες θα αρέσει πολύ η στάση του μετρό που υπάρχει σε ελάχιστη απόσταση από το ξενοδοχείο, παρέχοντας εύκολη πρόσβαση στις επιχειρηματικές περιοχές της πόλης και σε διάσημα αξιοθέατα.\n" +
                        "\n" +
                        "Η περιοχή 9ο διαμ. είναι υπέροχη επιλογή για ταξιδιώτες που ενδιαφέρονται για: ψώνια, τέχνη και αρχιτεκτονική.\n" +
                        "\n" +
                        "Αυτό είναι το αγαπημένο μέρος των επισκεπτών μας στον προορισμό Παρίσι σύμφωνα με ανεξάρτητα σχόλια."
        );


        for (int country = 0; country < 4; country++)
            for (int i = 0; i < numOfHotels; i++) {
                hotel = new Hotel(business, names.get(country * numOfHotels + i),
                        "info@" + String.format("hotel<%d>_%d", country + 1, i + 1) + ".com", numOfRooms,
                        countrieslat[country] + 0.001 * i, countrieslng[country] + 0.001 * i,
                        "Short Description " + i, descriptionLongs.get(country * numOfHotels + i), i % 3 + 2.5);
                rooms = new ArrayList<>();  // (Re)declare the list to add the new rooms (and throw away the previous).

                for (int j = 0; j < numOfRooms; j++) {

                    int interval = 0;

                    room = new Room((j + 1), hotel, 2 + ((j + i) % 4), 50 + ((j + i) % 6) * 50);
                    /*List<Calendar> calendars = new ArrayList<>();  // (Re)declare the list to add the new calendars (and throw away the previous).

                    // rooms will be booked from ( 2 days from now  to  30 days from now )
                    for (int k = 0; k < numOfCalendarsEntriesPerRoom; k++) {
                        LocalDate startDate = LocalDate.now().plusMonths(interval);
                        LocalDate endDate = LocalDate.now().plusMonths(interval+1);

                        calendars.add(new Calendar(startDate, endDate, room));
                        interval = interval + 2;
                    }
                    room.setCalendars(calendars);*/
                    rooms.add(room);
                }
                hotel.setRooms(rooms);

                Collection<Amenity> amenities = new ArrayList<>(amenitiesRepository.findAll());
                hotel.setAmenities(amenities);

                hotels.add(hotel);
            }

        // Assign the hotels to the business
        business.setHotels(hotels);

        return business;
    }

    @Test
    public void createRandomUsers() {

        /* Create dummy users */
        int number_of_users = 30;
        Role role = roleRepository.findByName(RoleName.ROLE_USER);

        for (int i = 0; i < number_of_users; i++) {

            if (i == number_of_users / 2)
                role = roleRepository.findByName(RoleName.ROLE_PROVIDER);

            String username = "user_" + (i + 1);
            String email = "emailU" + (i + 1) + "@mail.com";

            // Continue if already exists
            if (userRepository.findByUsernameOrEmail(username, email).isPresent())
                continue;

            User user = new User(
                    null,
                    username,
                    passwordEncoder.encode("asdfk2.daADd"),
                    email,
                    role,
                    false,
                    false,
                    null,
                    null
            );

            String name = "Rent_" + i;
            String surname = "Cube_" + i;
            Profile profile = new Profile(
                    user,
                    name,
                    surname,
                    new Date(),
                    "https://ui-avatars.com/api/?name=" + name + "+" + surname + "&rounded=true&%20bold=true&" +
                            "background=a8d267&color=00000" + 1
            );
            user.setProfile(profile);

            // Create wallet
            //half of them will have 1000 balance, others 1000
            //Providers also have money and a wallet, but no business       (TODO)
            Wallet wallet = new Wallet(user, (double) (1000 + ((i % 2 == 0 ? 1000 : 0))));
            user.setWallet(wallet);

            userRepository.save(user);
        }
    }

    //@Test
    public void insertHotel(HotelRepository hotelRepository, BusinessRepository businessRepository, RoomRepository roomRepository) {
        // Get a business for the hotel:
        Business business = businessRepository.findById((long) 1).orElse(null);
        // Create the hotel:
        Hotel newHotel = new Hotel(business,
                "Blue Dolphin",
                "info@blue_dolphin.com",
                100,
                30.8,
                25.7,
                "Nice hotel",
                "Very nice hotel",
                4.3);

        hotelRepository.save(newHotel);

        // Create the rooms:
        for (int i = 1; i <= 30; i++) {
            Room room = new Room(i, newHotel, 2, 100);
            roomRepository.save(room);
        }
        for (int i = 1; i <= 30; i++) {
            Room room = new Room(i, newHotel, 3, 100);
            roomRepository.save(room);
        }
        for (int i = 1; i <= 30; i++) {
            Room room = new Room(i, newHotel, 4, 100);
            roomRepository.save(room);
        }
    }

}
