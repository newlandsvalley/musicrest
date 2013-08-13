var arr;
 var option;

 // dynamic selection drop-downs
 function rhythmChange(genre) {
   var rhythm_dropdown = document.getElementById("rhythm_dropdown");   
   // Set each option to null thus removing it
   while ( rhythm_dropdown.options.length ) rhythm_dropdown.options[0] = null;

   switch (genre) {   
 
    case "irish":
      arr = new Array("any","jig","reel","hornpipe","barndance","highland","march","mazurka","polka","slide","slip jig","waltz");
    break;

    case "scottish":
      arr = new Array("any","jig","reel","hornpipe","barndance","march","schottische","slip jig","strathspey","waltz");
    break;

    case "scandi":
      arr = new Array("any","polska","brudmarsch","gånglåt","slängpolska","långdans","marsch","schottis","waltz");
    break;

    case "klezmer":
      arr = new Array("any","bulgar","freylekhs","khosidl","hora","csardas","doina","honga","hopak","kasatchok","kolomeyke","sher","sirba","skotshne","taksim","terkish");
    break;
    
    default:
      arr = new Array("")     
    break; 
   }   
   for (var i=0;i<arr.length;i++) {
     option = new Option(arr[i],arr[i]);
     rhythm_dropdown.options[i] = option;
   }
   rhythm_dropdown.disabled = false;
  }
   
  function selectGenre(genre) {    
    var element = document.getElementById('genre');
    element.value = genre;
  }

  function genreInit(genre) {
     selectGenre(genre)
     var genre_dropdown = document.getElementById("genre");   
     rhythmChange(genre)
  }
 
