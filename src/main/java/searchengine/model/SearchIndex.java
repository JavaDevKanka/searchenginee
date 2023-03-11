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
//import javax.persistence.Index;
//import javax.persistence.Table;
//
//@Entity
//@Getter
//@Setter
//@Table(
//        indexes = {
//                @Index(columnList = "")
//        }
//)
//public class SearchIndex {
//    @Id
//    @Column(nullable = false)
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int Id;
//
//    @Column(nullable = false)
//    private int pageId;
//
//    @Column(nullable = false)
//    private int lemmaId;
//
//    @Column(nullable = false)
//    private float rank;
//}
