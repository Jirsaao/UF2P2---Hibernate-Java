package manager;
import java.util.ArrayList;
import org.bson.Document;
import java.util.List;

import javax.print.Doc;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
 
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
 
import model.Bodega;
import model.Campo;
import model.Entrada;
import model.Vid;
import utils.TipoVid;


public class Manager {
	private static Manager manager;
	private ArrayList<Entrada> entradas;
	private Session session;
	private Transaction tx;
	private Bodega b;
	private Campo c;
	private MongoDatabase database;
	private MongoCollection<org.bson.Document> collection;

	private Manager () {
		this.entradas = new ArrayList<>();
	}
	
	public static Manager getInstance() {
		if (manager == null) {
			manager = new Manager();
		}
		return manager;
	}
	
	private void createSession() {
			Configuration configuration = new Configuration().configure("hibernate.cfg.xml"); 
			org.hibernate.SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
			session = sessionFactory.openSession();
			String uri= "mongodb://localhost:27017";
			MongoClientURI mongoClientURI = new MongoClientURI(uri);
			MongoClient mongoClient = new MongoClient(mongoClientURI);
			database = mongoClient.getDatabase("winery");
	}

	public void init() {	
		createSession();
		getEntrada();
		manageActions();
		showAllCampos();
		session.close();
	}

	private void manageActions() {
		for (Entrada entrada : this.entradas) {
			try {
				System.out.println(entrada.getInstruccion());
				switch (entrada.getInstruccion().toUpperCase().split(" ")[0]) {
					case "B":
						addBodega(entrada.getInstruccion().split(" "));
						break;
					case "C":
						addCampo(entrada.getInstruccion().split(" "));
						break;
					case "V":
						addVid(entrada.getInstruccion().split(" "));
						break;
					// case "#":
					// 	vendimia();
					// 	break;
					default:
						System.out.println("Instruccion incorrecta");
				}
			} catch (HibernateException e) {
				e.printStackTrace();
				if (tx != null) {
					tx.rollback();
				}
			}
		}
	}

	// private void vendimia() {
	// 	this.b.getVids().addAll(this.c.getVids());
		
	// 	tx = session.beginTransaction();
	// 	session.save(b);
		
	// 	tx.commit();
	// }

    private void addVid(String[] split) {

		System.out.println("vid añadido: " );
		collection = database.getCollection("campo");
		Document lastCampo = collection.find().sort(new Document("_id", -1)).first();
		
		collection = database.getCollection("vid");
		Document document  = new Document().append("tipo_vid", split[1].toString())
		.append("cantidad",Integer.parseInt(split[2])).append("campo", lastCampo);
		collection.insertOne(document);

	}
	
	private void addCampo(String[] split) {
		Campo c = new Campo(b, split[1]);
		collection = database.getCollection("bodega");//añado la ultima bodega
		Document lastBodega = collection.find().sort(new Document("_id", -1)).first();

		Document campoDoc = new Document("nombre", c.getNombre()).append("Bodega", lastBodega);
		database.getCollection("campo").insertOne(campoDoc);
	
	}

	 private void addBodega(String[] split) {
		Bodega b = new Bodega(split[1]);
	
		Document bodegaDoc = new Document("nombre", b.getNombre());
		database.getCollection("bodega").insertOne(bodegaDoc);
		
	}
	
	private ArrayList<Entrada> getEntrada() {
		collection = database.getCollection("entrada");
		this.entradas = new ArrayList<>();
		for (Document document : collection.find()){
			Entrada input = new Entrada();
			input.setInstruccion(document.getString("instruccion"));
			this.entradas.add(input);
		}
		return entradas;
	}

	private void showAllCampos() {
		tx = session.beginTransaction();
		Query q = session.createQuery("select c from Campo c");
		List<Campo> list = q.list();
		for (Campo c : list) {
			System.out.println(c);
		}
		tx.commit();
	}

}
