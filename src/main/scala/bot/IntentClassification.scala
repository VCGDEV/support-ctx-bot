package bot

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class for bot responses
  * */
class IntentClassification(val tag:String, val mainCategoryId:String, val categoryId:String){

  override def toString: String ={
    s"""TAG:$tag - CATEGORY: $mainCategoryId - SUBCATEGORY: $categoryId"""
  }
}
