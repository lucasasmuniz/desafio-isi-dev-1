import './styles.css'

type Props = {
    text: string;
}

export default function ButtonSecondary({text}: Props){
    return(
        <button className='button-secondary'>{text}</button>
    );
}