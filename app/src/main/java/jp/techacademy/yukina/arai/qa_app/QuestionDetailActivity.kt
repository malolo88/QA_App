package jp.techacademy.yukina.arai.qa_app

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import java.util.*
import kotlin.collections.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavorite: DataSnapshot

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()


        }


        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title


        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        //お気に入りボタンを押した時
        favorite_button.setOnClickListener{v ->
            //ログインしているユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser
            val userId = user!!.uid

            //DatabaseReferenceのインスタンスを取得
            val databaseRef = FirebaseDatabase.getInstance().getReference()

            if (mFavorite.hasChild(mQuestion.questionUid)){
                //お気に入りから削除する
                databaseRef.child(FavoritesPATH).child(userId).child(mQuestion.questionUid).removeValue()
                Snackbar.make(v, "お気に入りから削除しました", Snackbar.LENGTH_LONG).show()

                //ボタンをお気に入り未登録（白）にかえる
                favorite_button.setImageResource(R.drawable.like_white)
            } else {
                //お気に入りに登録する
                val favoriteRef = databaseRef.child(FavoritesPATH).child(userId).child(mQuestion.questionUid)
                val data = HashMap<String, String>()
                data["genre"] = mQuestion.genre.toString()
                favoriteRef.setValue(data)
                Snackbar.make(v, "お気に入りに登録しました", Snackbar.LENGTH_LONG).show()

                //ボタンをお気に入り登録済み（ピンク）にかえる
                favorite_button.setImageResource(R.drawable.like_pink)
            }
        }

        if (mQuestion.genre == 5){
            fab.hide()
        }
    }

    override fun onResume() {
        super.onResume()

        //ユーザーがログイン済みでなければお気に入りボタンを非表示にする
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null){
            favorite_button.visibility = View.INVISIBLE
        } else {
            favorite_button.visibility = View.VISIBLE

            //お気に入りに登録されているかのチェック
            val database = FirebaseDatabase.getInstance().getReference()
            database.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userId = user!!.uid
                    mFavorite = dataSnapshot.child(FavoritesPATH).child(userId)

                    if (mFavorite.hasChild(mQuestion.questionUid)){
                        //ボタンをお気に入り登録済み（ピンク）にかえる
                        favorite_button.setImageResource(R.drawable.like_pink)
                    } else {
                        //ボタンをお気に入り未登録（白）にかえる
                        favorite_button.setImageResource(R.drawable.like_white)
                    }
                }
                override fun onCancelled(p0: DatabaseError) {

                }
            })
        }
    }
}
