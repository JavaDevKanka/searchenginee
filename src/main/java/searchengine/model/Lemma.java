//package searchengine.model;
//
//import lombok.Getter;
//import lombok.Setter;
//
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//
//@Entity
//@Getter
//@Setter
//public class Lemma {
//    @Id
//    @Column(nullable = false)
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int Id;
//
//    @Column(nullable = false)
//    private int siteId;
//
//    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
//    private String lemma;
//
//    @Column(nullable = false)
//    private int frequency;
//}
