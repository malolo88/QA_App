package jp.techacademy.yukina.arai.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.support.design.widget.Snackbar
import android.util.Base64
import android.util.Log
import android.widget.ListView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class MainActivity : AppCompatActivity() ,NavigationView.OnNavigationItemSelectedListener{

    private lateinit var mToolbar : Toolbar
    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter


    private var mGenreRef: DatabaseReference? = null
    private var mFavoritesRef: DatabaseReference? = null
    private var mMenuId =0


    private val mEventListener = object : ChildEventListener{
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"]?: ""
            val body = map["body"]?: ""
            val name = map["name"]?: ""
            val uid = map["uid"]?: ""
            val imageString = map["image"]?: ""
            val bytes =
                if (imageString.isNotEmpty()){
                    Base64.decode(imageString, Base64.DEFAULT)
                } else{
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null){
                for (key in answerMap.keys){
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"]?: ""
                    val answerName = temp["name"]?: ""
                    val answerUid = temp["uid"]?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "", mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            //変更があったQuestionを探す
            for (question in mQuestionArrayList){
                if (dataSnapshot.key.equals(question.questionUid)){
                    //このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null){
                        for (key in answerMap.keys){
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"]?: ""
                            val answerName = temp["name"]?: ""
                            val answerUid = temp["uid"]?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            //ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            if (mGenre == 0) {
                Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show()
            } else {

            }

            //ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                //ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                //ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        //ナビゲーションドロワーの設定
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle =
            ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        //Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        //ListViewの準備
        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)

        }

    }

    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        //1:趣味を既定の選択とする
        if (mGenre == 0){
            onNavigationItemSelected(navigationView.menu.getItem(1))
        }

        //お気に入りの表示
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null){
            //お気に入りを非表示にする
            val menu = navigationView.menu
            val menuFavorites = menu.findItem(R.id.nav_favorites)
            menuFavorites.setVisible(false)
        } else {
            //お気に入りを表示する
            val menu = navigationView.menu
            val menuFavorites = menu.findItem(R.id.nav_favorites)
            menuFavorites.setVisible(true)
        }

        val menuFavorites = navigationView.menu.findItem(R.id.nav_favorites)
        val menuFavoritesId = menuFavorites.itemId

        //お気に入りの表示
        if (menuFavoritesId == mMenuId){
            if (mFavoritesRef != null){
                //Floating Action Buttonを非表示にする
                fab.hide()

                //質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                mQuestionArrayList.clear()
                mAdapter.setQuestionArrayList(mQuestionArrayList)
                mListView.adapter = mAdapter

                FavoritesList()
                }
            } else {
                //それぞれのジャンルの表示
                if (mGenreRef != null){
                    //Floating Action Buttonを表示にする
                    fab.show()
                    mGenreRef!!.removeEventListener(mEventListener)

                    //質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                    mQuestionArrayList.clear()
                    mAdapter.setQuestionArrayList(mQuestionArrayList)
                    mListView.adapter = mAdapter

                    mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
                    mGenreRef!!.addChildEventListener(mEventListener)
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings){
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby){
            mToolbar.title = "趣味"
            mGenre = 1
            mMenuId = R.id.nav_hobby
        } else if (id == R.id.nav_life){
            mToolbar.title = "生活"
            mGenre = 2
            mMenuId = R.id.nav_life
        } else if (id == R.id.nav_health){
            mToolbar.title = "健康"
            mGenre = 3
            mMenuId = R.id.nav_health
        } else if (id == R.id.nav_computer){
            mToolbar.title = "コンピューター"
            mGenre = 4
            mMenuId = R.id.nav_computer
        } else if (id == R.id.nav_favorites){
            mToolbar.title = "お気に入り"
            mGenre = 5
            mMenuId = R.id.nav_favorites
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        //質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        //お気に入りの表示
        if (mMenuId == R.id.nav_favorites){
            //Floating Action Buttonを非表示にする
            fab.hide()
            FavoritesList()
        } else {
            fab.show()
            if (mGenreRef != null){
                mGenreRef!!.removeEventListener(mEventListener)
            }

            //選択したジャンルにリスナーを登録する
            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
            mGenreRef!!.addChildEventListener(mEventListener)

        }
        return true
    }

    //お気に入りのリストの表示
    private fun FavoritesList(){
        if (mFavoritesRef != null){
            mFavoritesRef!!.removeEventListener(mEventListener)
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference()
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = user!!.uid
                    val map = dataSnapshot.child(FavoritesPATH).value as Map<String, String>
                    val favoritesMap = map[userId] as Map<String, String>?
                    if (favoritesMap != null) {
                        for (key in favoritesMap.keys) {
                            val temp = favoritesMap[key] as Map<String, String>
                            val favoritesQuestionId = key
                            val favoritesGenres = temp["genre"] ?: ""

                            mFavoritesRef = databaseRef.child(ContentsPATH).child(favoritesGenres)
                            mFavoritesRef!!.orderByKey().equalTo(favoritesQuestionId).addChildEventListener(mEventListener)
                        }

                    }
                } catch (e: Exception){

                }
            }

            override fun onCancelled(dabaseError: DatabaseError) {

            }
        })

    }

}
