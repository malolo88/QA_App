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

        showFavoriteButton() //お気に入りボタンの表示・非表示

        //お気に入りボタンを押した時
        favorite_button.setOnClickListener{v ->
            //ログインしているユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser
            val userId = user!!.uid

            //DatabaseReferenceのインスタンスを取得
            val favoritRef = FirebaseDatabase.getInstance().getReference()

            if (mFavorite.hasChild(mQuestion.questionUid)){
                //お気に入りから削除する
                favoritRef.child(FavoritesPATH).child(userId).child(mQuestion.questionUid).removeValue()
                Snackbar.make(v, "お気に入りから削除しました", Snackbar.LENGTH_LONG).show()

                //ボタンをお気に入り未登録（白）にかえる
                favoriteButtonWhite()
            } else {
                //お気に入りに登録する
                favoritRef.child(FavoritesPATH).child(userId).child(mQuestion.questionUid).child("genre").setValue(mQuestion.genre)
                Snackbar.make(v, "お気に入りに登録しました", Snackbar.LENGTH_LONG).show()

                //ボタンをお気に入り登録済み（ピンク）にかえる
                favoriteButtonPink()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        showFavoriteButton()
    }

    private fun showFavoriteButton(){
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
                       favoriteButtonPink()
                    } else {
                       //ボタンをお気に入り未登録（白）にかえる
                       favoriteButtonWhite()
                    }
                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
        }
    }

    //お気に入りを登録済みのボタンにする
    private fun favoriteButtonPink(){
        favorite_button.visibility = View.INVISIBLE
        favorite_button2.visibility = View.VISIBLE
    }

    //お気に入りを未登録のボタンにする
    private fun favoriteButtonWhite(){
        favorite_button.visibility = View.VISIBLE
        favorite_button2.visibility = View.INVISIBLE
    }

}
